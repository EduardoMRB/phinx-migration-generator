(ns migration-generator.core
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [selmer.parser :as selmer]))

(defn get-tables
  "Retrieves a list containing all tables in the database."
  [db]
  (map (comp second first)
       (j/query db "show tables")))

(defn column-usage
  "Gets all the foreign keys declarations for a particular schema and table."
  [db schema table]
  (j/query db ["select *
                from INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                where table_name = ?
                and referenced_table_name is not null
                and table_schema = ?
                and column_name is not null
                and referenced_column_name is not null
                and constraint_name <> 'PRIMARY'"
               table
               schema]))

(defn describe-tables
  "Retrieves information regarding all the tables passed as argument."
  [db tables]
  (map (fn [table]
         (conj
          (j/query db (str "describe " table))
          {:type "table" :name table :key ""}))
       tables))

(defn column-type
  "Translates the MySQL column type to Phinx column type."
  [{:keys [type]}]
  (cond
    (re-find #"int\(\d+\)" (str type))      "integer"
    (= "datetime" type)                     "datetime"
    (= "date" type)                         "date"
    (re-find #"varchar\(\d+\)" (str type))  "string"
    (= "longtext" type)                     "text"
    (re-find #"enum(.*)" (str type))        "enum"
    (re-find #"char\(\d+\)" (str type))     "string"
    (re-find #"tinyint\(\d+\)" (str type))  "integer"
    (re-find #"float\((.*)\)" (str type))   "float"
    (re-find #"decimal\((.*)\)" (str type)) "decimal"
    :else                                   (str type)))

(defn enum-values [{:keys [type]}]
  (apply str (remove #(= \' %)
                     (second (re-matches #"enum\((.*)\)" (str type))))))

(defn- decimal-values [{:keys [type]}]
  (let [[precision scale] (-> (re-matches #"decimal\((.*)\)" (str type))
                               (second)
                               (str/split #","))]
    {:precision precision
     :scale scale}))

(defn column-data [column]
  (let [col-type (column-type column)]
    {:name    (:field column)
     :type    col-type
     :options (cond-> [["null" (= "YES" (:null column))]]
                (= "decimal" col-type)               (conj ["precision" (:precision (decimal-values column))])
                (= "decimal" col-type)               (conj ["scale" (:scale (decimal-values column))])
                (= "enum" col-type)                  (conj ["values" (enum-values column)])
                (not (str/blank? (:default column))) (conj ["default" (:default column)]))}))

(defn camelize-table [table-name]
  (let [parts (str/split table-name #"_")]
    (reduce str (map str/capitalize parts))))

(defn table-data [db schema table]
  (let [pk-entry   (->> table
                        (filter #(= (:key %) "PRI"))
                        first)

        table-data (->> table
                        (filter #(= "table" (:type %)))
                        first)

        without-pk (->> table
                        (remove #(= pk-entry %))
                        (remove #(= table-data %)))

        fks        (->> (:name table-data)
                        (column-usage db schema)
                        (map (fn [info]
                               {:col-name            (:column_name info)
                                :referenced-table    (:referenced_table_name info)
                                :referenced-col-name (:referenced_column_name info)})))
        ]
    {:pk (:name (column-data pk-entry))
     :camelized-table (camelize-table (:name table-data))
     :table-name (:name table-data)
     :fields (map column-data without-pk)
     :fks fks}))

(defn render-migration-content
  ([table-data]
   (render-migration-content table-data true))
  ([table-data generate-fks]
   (let [table-data (if generate-fks
                      table-data
                      (assoc table-data :fks []))]
     (selmer/render
      "<?php

use Phinx\\Migration\\AbstractMigration;

class Create{{camelized-table}} extends AbstractMigration
{
    public function change()
    {
        $table = $this->table('{{table-name}}', [
            'id' => '{{pk}}',
        ]);

        $table
        {% for field in fields %}
            ->addColumn('{{field.name}}', '{{field.type}}', [
                {% for option in field.options %}
                '{{option.0}}' => '{{option.1}}',
                {% endfor %}
            ])
        {% endfor %}
        {% for fk in fks %}
            ->addForeignKey('{{fk.col-name}}', '{{fk.referenced-table}}', '{{fk.referenced-col-name}}')
        {% endfor %}
            ->create();
    }
}"
      table-data))))

(defn migration-filename [table-name]
  (let [fmt (java.text.SimpleDateFormat. "yyyyMMddHHmmss")
        ts  (.format fmt (java.util.Date.))
        name-parts (str/split table-name #"_")
        migration-name (str/join "_" (cons "create" (if (= (first name-parts) "")
                                                      (rest name-parts)
                                                      name-parts)))]
    (str ts "_"  migration-name ".php")))

(defn -main
  "Takes a MySQL database connection configuration and a directory to output
  the Phinx migrations."
  [host dbname user pass schema directory generate-fk & more]
  (let [db           {:subprotocol "mysql"
                      :subname (str "//" host ":3306/" dbname)
                      :user user
                      :password pass}
        generate-fks (if (= "true" generate-fk)
                       true
                       false)
        tables       (map #(table-data db schema %)
                          (describe-tables db (get-tables db)))]
    (doseq [table tables]
      (let [fcontent (render-migration-content table generate-fks)
            fname    (migration-filename (:table-name table))
            _        (Thread/sleep 1000) ; We need one second delay so the migration timestamp don't repeat!
            ]
        (println "Creating migration: " fname)
        (spit (str directory "/" fname) fcontent)
        (println "Created!")))
    (println "Done!")))

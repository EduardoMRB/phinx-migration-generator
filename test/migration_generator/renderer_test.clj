(ns migration-generator.renderer-test
  (:require [migration-generator.renderer :refer :all]
            [clojure.test :refer :all]))

(def rendered-without-fks
  "<?php

use Phinx\\Migration\\AbstractMigration;

class CreateTestTable extends AbstractMigration
{
    public function change()
    {
        $table = $this->table('test_table', [
            'id' => 'id',
        ]);

        $table
            ->addColumn('test_field', 'integer', [
                'null' => true,
                'default' => '1',
            ])
            ->create();
    }
}")

(def rendered-with-fks
  "<?php

use Phinx\\Migration\\AbstractMigration;

class CreateTestTable extends AbstractMigration
{
    public function change()
    {
        $table = $this->table('test_table', [
            'id' => 'id',
        ]);

        $table
            ->addColumn('test_field', 'integer', [
                'null' => true,
                'default' => '1',
            ])
            ->addForeignKey('phone_id', 'phone', 'id')
            ->create();
    }
}")

(defn- template=? [expected actual]
  (letfn [(trimmed [template]
            (remove #(or (= \newline %) (= \space %))
                    template))]
    (= (trimmed expected) (trimmed actual))))

(deftest render-migration-content-test
  (let [without-fks {:table-name "test_table"
                     :pk "id"
                     :camelized-table "TestTable"
                     :fields [{:name "test_field"
                               :type "integer"
                               :options [["null" true]
                                         ["default" "1"]]}]
                     :fks []}]
    (testing "tables without foreign keys are rendered"
      (is (template=? rendered-without-fks (render-migration-content without-fks)))
      (is (template=? rendered-without-fks
                      (render-migration-content
                       (assoc without-fks
                              :fks [{:col-name "phone_id"
                                     :referenced-table "phone"
                                     :referenced-col-name "id"}])
                       false))))
    (testing "tables with foreign keys are rendered"
      (is (template=? rendered-with-fks
                      (render-migration-content
                       (assoc without-fks
                              :fks [{:col-name "phone_id"
                                     :referenced-table "phone"
                                     :referenced-col-name "id"}])))))))

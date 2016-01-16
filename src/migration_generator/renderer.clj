(ns migration-generator.renderer
  (:require [clojure.java.io :as io]
            [selmer.filters :as filters]
            [selmer.parser :as selmer]))

(filters/add-filter!
 :bool?
 (fn [x]
   (= (type x) java.lang.Boolean)))

(selmer/set-resource-path! (io/resource "templates"))

(defn render-migration-content
  ([table-data]
   (render-migration-content table-data true))
  ([table-data generate-fks]
   (let [table-data (if generate-fks
                      table-data
                      (assoc table-data :fks []))]
     (selmer/render-file
      "abstract_migration.html"
      table-data))))

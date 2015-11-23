(ns migration-generator.core-test
  (:require [clojure.test :refer :all]
            [migration-generator.core :refer :all]))

(deftest test-column-data
  (testing "Precision and scale are calculated correctly for decimal columns"
    (let [column {:field "test_field"
                  :type "decimal(14,2)"
                  :null "NO"
                  :default nil}]
      (is (= {:name "test_field"
              :type "decimal"
              :options [["null" false]
                        ["precision" "14"]
                        ["scale" "2"]]}
             (column-data column)))))
  (testing "Support for varchar length"
    (let [column {:field "test_varchar"
                  :type "varchar(300)"
                  :null "NO"
                  :default nil}]
      (is (= {:name "test_varchar"
              :type "string"
              :options [["null" false]
                        ["length" "300"]]}
             (column-data column))))))

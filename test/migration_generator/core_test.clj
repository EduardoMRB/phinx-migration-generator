(ns migration-generator.core-test
  (:require [clojure.test :refer :all]
            [migration-generator.core :refer :all]))

(deftest test-column-data
  (testing "Precision and scale are calculated correctly for decimal columns"
    (let [column {:field   "test_field"
                  :type    "decimal(14,2)"
                  :null    "NO"
                  :default nil}]
      (is (= {:name    "test_field"
              :type    "decimal"
              :options [["null" false]
                        ["precision" "14"]
                        ["scale" "2"]]}
             (column-data column)))))
  (testing "Support for varchar length"
    (let [column {:field   "test_varchar"
                  :type    "varchar(300)"
                  :null    "NO"
                  :default nil}]
      (is (= {:name    "test_varchar"
              :type    "string"
              :options [["null" false]
                        ["length" "300"]]}
             (column-data column)))))
  (testing "Support for integer length"
    (let [column {:field   "test_field"
                  :type    "int(50)"
                  :null    "NO"
                  :default nil}]
      (is (= {:name    "test_field"
              :type    "integer"
              :options [["null" false]
                        ["length" "50"]]}
             (column-data column)))))
  (testing "Tinyint columns are converted to boolean type"
    (let [column {:field   "test_field"
                  :type    "tinyint(1)"
                  :null    "YES"
                  :default nil}]
      (is (= {:name    "test_field"
              :type    "boolean"
              :options [["null" true]]}
             (column-data column)))))
  (testing "Tinyints larger than 1 are converted to integers"
    (let [column {:field   "test_field"
                  :type    "tinyint(3)"
                  :null    "YES"
                  :default nil}]
      (is (= {:name    "test_field"
              :type    "integer"
              :options [["null" true]
                        ["length" "3"]]}
             (column-data column))))
    )
  (testing "Support for longtext columns"
    (let [column {:field   "test_field"
                  :type    "longtext"
                  :null    "NO"
                  :default nil}]
      (is (= {:name    "test_field"
              :type    "text"
              :options [["null" false]
                        ["limit" 4294967295]]}
             (column-data column)))))
  (testing "Support for year fields"
    (let [column {:field "idade"
                  :type "year(4)"
                  :null "NO"
                  :default nil}]
      (is (= {:name "idade"
              :type "integer"
              :options [["null" false]
                        ["length" "4"]]}
             (column-data column))))))

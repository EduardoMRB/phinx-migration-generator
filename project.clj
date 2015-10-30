(defproject migration-generator "0.1.0-SNAPSHOT"
  :description "Generates migrations for Phinx"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojars.kjw/mysql-connector "5.1.11"]
                 [selmer "0.9.3"]]
  :main migration-generator.core)

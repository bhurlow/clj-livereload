(defproject clj-livereload "0.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"]
                 [ring "1.4.0"]
                 [http-kit "2.1.19"]
                 [hawk "0.2.4"]
                 [org.webjars.npm/livereload-js "2.2.2"]]
  :profiles {:example {:resource-paths ["example-resources"]
                       :source-paths ["example"]
                       :dependencies [[compojure "1.4.0"]]
                       :main clj-livereload.test}}
  :aliases {"start-example" ["with-profile" "example" "run"]})

(ns clj-livereload.test
  (:require [clj-livereload.server :refer :all]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer :all]
            [org.httpkit.server :refer [run-server]]))

(println "Watching: " (.getAbsolutePath (clojure.java.io/file "example-resources/public")))

(start! {:paths ["example-resources/public"]
         :debug? true})

(defroutes routes
  (GET "/" req (resource-response "index.html" {:root "public"}))
  (resources "/"))

(defn -main []
  (println "Starting http server on http://localhost:8080")
  (run-server routes {:port 8080}))

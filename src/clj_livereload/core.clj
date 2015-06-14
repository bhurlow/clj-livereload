(ns clj-livereload.core
  (:require [compojure.core :refer [context defroutes GET OPTIONS POST]]
            [org.httpkit.server :refer [run-server with-channel on-close on-receive send! open?]]
            [ring.middleware.reload :refer :all]
            [ring.util.response :refer :all]
            [cheshire.core :as json]
            [hawk.core :as hawk])
  (:use clojure.pprint))

(def watcher (atom nil))
(def reload-channel (atom nil))

(defn- hello-message []
  {:command "hello"
   :protocols ["http://livereload.com/protocols/official-7"
               "http://livereload.com/protocols/official-8"
               "http://livereload.com/protocols/official-9"
               "http://livereload.com/protocols/2.x-origin-version-negotiation"
               "http://livereload.com/protocols/2.x-remote-control"]
   :serverName "clj-livereload"})

(defn- send-reload-msg [path]
  (println "trigggered reload for path" path)
  (when (and @reload-channel (open? @reload-channel))
    (send! @reload-channel 
           (json/generate-string
             {:command "reload"
              :path (str "/" path)
              :liveCSS true }))))

(defn cssFile? [ctx e]
  (.endsWith (.getName (:file e)) ".css"))

(defn- watch-params [paths]
  [{:paths [paths]
    :filter cssFile?
    :handler (fn [ctx e]
               (println "detected file change")
               (send-reload-msg (.getName (:file e))))}])

(defn- watch-directory [dir]
  (println "starting to watch dir..")
  (when @watcher (hawk/stop! @watcher))
  (reset! watcher (hawk/watch! (watch-params dir))))

(defn- handle-livereload [req]
  (with-channel req channel
    (reset! reload-channel channel)
    (on-receive channel 
                (fn [data]
                  (let [parsed (json/decode data true)]
                    (println parsed)
                    (case (:command parsed)
                      "hello" (send! channel (json/generate-string (hello-message)))
                      nil))))))

(defn- send-livereload-js [req]
  (-> (resource-response "livereload.js")
      (content-type "application/javascript")))

(defroutes routes
  (GET "/livereload.js" req (send-livereload-js req))
  (GET "/livereload" req (handle-livereload req))
  (GET "/style.css*" req (resource-response "style.css"))
  (GET "/" req (resource-response "index.html")))

(def app (-> #'routes))

(defonce server (atom nil))

(defn- start-server []
  (let [port 35729]
    (->> (run-server app {:port port})
         (reset! server))))

(defn- stop-server
  "Stops the server after 100ms."
  [] (when-not (nil? @server) (@server :timeout 100) (reset! server nil)))

(defn- start-debug-server []
  (->> (run-server (wrap-reload #'app '(hippo.core hippo.db))
                   {:port 35729 :join? true})
       (reset! server)))

(defn livereload [dir]
  (when-not @server 
    (println "starting livereload server")
    (start-debug-server))
  (watch-directory dir))

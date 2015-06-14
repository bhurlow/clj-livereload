(ns clj-livereload.core
  (:require [compojure.core :refer [routes GET]]
            [org.httpkit.server :refer [run-server with-channel on-close on-receive send! open?]]
            [ring.middleware.reload :refer :all]
            [ring.util.response :refer :all]
            [cheshire.core :as json]
            [hawk.core :as hawk]))

(defn- info [state & args] (if-not (:silent? @state) (apply println args)))
(defn- dbug [state & args] (if (:debug? @state) (apply println args)))

(defn- hello-message []
  {:command "hello"
   ; Tiny-lr supports only this, so I think it's safe to do the same
   :protocols ["http://livereload.com/protocols/official-7"]
   :serverName "clj-livereload"})

(defn css-file? [path]
  (.endsWith path ".css"))

(defn send-reload-msg
  "Given state and path, send reload message to the clients."
  [state path]
  (info state "Reloading:" path)
  (dbug state "Sending changes to" (count (:reload-channels @state)) "clients")
  (doseq [channel (:reload-channels @state)]
    (if (open? channel)
      (send! channel
             (json/generate-string
               {:command "reload"
                :path (str "/" path)
                :liveCSS true}))
      (swap! state update-in [:reload-channels disj channel]))))

(defn- watch-directory [state paths]
  (info state "Watching directories...")
  (hawk/watch!
    [{:paths paths
      :filter hawk/file?
      :handler (fn [ctx e]
                 (send-reload-msg state (.getName (:file e)))
                 ctx)}]))

(defn- handle-livereload [state req]
  (with-channel req channel
    (swap! state update-in [:reload-channels] conj channel)
    (on-receive
      channel
      (fn [data]
        (let [parsed (json/decode data true)]
          (dbug state "Received message:" parsed)
          (case (:command parsed)
            "hello" (send! channel (json/generate-string (hello-message)))
            nil))))
    (on-close
      channel
      (fn [_]
        (swap! state update-in [:reload-channels] disj channel)))))

(defn- handler [state]
  (routes
    (GET "/livereload.js" req
      (-> (resource-response "META-INF/resources/webjars/livereload-js/2.2.2/dist/livereload.js" {:root ""})
          (content-type "application/javascript")))
    (GET "/livereload" req
      (handle-livereload state req))))

(defn create-state
  "Creates the state holding open websocket connections."
  [opts]
  (atom (merge (select-keys opts [:debug? :silent?])
               {:reload-channels #{}})))

(def default-port 35729)

(defn start
  "Starts the http-server and (by default) watch service.
   Returns a map containg the server state. It can be passed to
   stop function to stop the server.

   Options:
   - :paths    Directories to watch for changes. If empty, watch service is not used.
   - :port     Default 35729.
   - :silent?  Disable output.
   - :deblug?  Enable debug output."
  [{:keys [paths port]
    :as opts}]
  (let [port (or port default-port)
        state (create-state opts)]
    (when-not (= port default-port)
      (println "Warn: LiveReload port is not standard (%s). You are listening on %d." default-port port)
      (println "You'll need to rely on the LiveReload snipper")
      (println "> http://feedback.livereload.com/knowledgebase/articles/86180-how-do-i-add-the-script-tag-manually-"))
    (info state "Starting LiveReload server")
    {:state state
     :http-kit (run-server (handler state) {:port port})
     :watch (if (seq paths)
              (watch-directory state paths))}))

(defn stop
  "Stop the http-server and watch service."
  [server]
  (if-let [watch (:watch server)]
    (hawk/stop! watch))
  (if-let [http-kit (:http-kit server)]
    (http-kit :timeout 100))
  nil)

(ns ring.middleware.jsonp
  (:import (java.io ByteArrayInputStream
                    File
                    FileInputStream
                    InputStream
                    SequenceInputStream)
           (java.nio.charset Charset)
           (clojure.lang Sequential
                         SeqEnumeration))
  (:use [clojure.string :only (lower-case)]))

(defn- get-param [request param]
  (or (get-in request [:params (keyword param)])
      (get-in request [:params (name    param)])))

(defn- re-matches? [^java.util.regex.Pattern re s]
  (-> (.matcher re s)
      (.matches)))

(defn- get-charset [content-type]
  (if-let [charset (re-find #"(?<=charset=)[^;]*" content-type)]
    (Charset/forName charset)
    (Charset/defaultCharset)))

(defn- get-content-type [response]
  (get-in response [:headers "Content-Type"] ""))

(defn- json-content-type? [content-type]
  (re-matches? #"application/(.*\+)?json(;.*)?" content-type))

(defprotocol Streamable
  (->stream [x]))

(def ^:private ^:dynamic ^Charset *current-charset*)

(extend-protocol Streamable
  ;; TODO: Add CollReduce when we depend on Clojure >= 1.4
  String      (->stream [x] (ByteArrayInputStream. (.getBytes x *current-charset*)))
  File        (->stream [x] (FileInputStream. x))
  InputStream (->stream [x] x)
  Sequential  (->stream [x] (->> x (map ->stream) SeqEnumeration. SequenceInputStream.))
  nil         (->stream [x] (->stream "")))

(defn- add-padding-to-json [callback content-type response]
  (binding [*current-charset* (get-charset content-type)]
    (-> response
        (assoc-in [:headers "Content-Type"] (str "application/javascript; charset=" (lower-case *current-charset*)))
        (update-in [:body] #(->stream [callback "(" % ");"])))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [callback (get-param request :callback)
          response (handler request)
          content-type (get-content-type response)]
      (if (and callback (json-content-type? content-type))
          (add-padding-to-json callback content-type response)
          response))))

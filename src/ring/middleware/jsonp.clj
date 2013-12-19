(ns ring.middleware.jsonp
  (:import (java.io ByteArrayInputStream
                    File
                    FileInputStream
                    InputStream
                    SequenceInputStream)
           (java.nio.charset Charset)
           (java.util Enumeration
                      NoSuchElementException)
           (clojure.lang SeqEnumeration))
  (:use [ring.util.response :only (response content-type)]))

(defn- get-param [request param]
  (or (get-in request [:params (keyword param)])
      (get-in request [:params (name    param)])))

(defn- re-matches? [^java.util.regex.Pattern re s]
  (.. re (matcher s) matches))

(defn- get-charset [content-type]
  (re-find #"(?<=charset=)[^;]*" content-type))

(defn- get-charset-or-default [content-type]
  (or (get-charset content-type)
      (.name (Charset/defaultCharset))))

(defn- get-content-type [response]
  (get-in response [:headers "Content-Type"] ""))

(defn- json-content-type? [content-type]
  (re-matches? #"application/(.*\+)?json(;.*)?" content-type))

(defn- pad-json? [callback response]
  (and callback (json-content-type? (get-content-type response))))

(defn- string->stream [^String s ^String charset]
  (ByteArrayInputStream. (.getBytes s charset)))

(defn- concat-streams [xs]
  (->> xs seq SeqEnumeration. SequenceInputStream.))

(defn- body->stream [body charset]
  (cond (seq? body) (concat-streams
                     (for [x body] (string->stream (str x) charset)))
        (instance? File body) (FileInputStream. body)
        (instance? InputStream body) body
        (string? body) (string->stream body charset)
        (nil? body) (string->stream "" charset)
        :else (throw (Exception. (str "Don't know how to convert "
                                      (type body)
                                      " to an InputStream!")))))

(defn- add-padding-to-json [callback response]
  (let [charset (get-charset-or-default (get-content-type response))]
    (-> response
        (content-type (str "application/javascript; charset=" charset))
        (update-in [:body]
                   #(concat-streams
                     [(string->stream (str callback "(") charset)
                      (body->stream % charset)
                      (string->stream ");" charset)])))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [callback (get-param request :callback)
          response (handler request)]
      (if (pad-json? callback response)
          (add-padding-to-json callback response)
          response))))

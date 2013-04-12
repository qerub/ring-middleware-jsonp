(ns ring.middleware.jsonp
  (:import (java.io ByteArrayInputStream
                    File
                    FileInputStream
                    InputStream
                    SequenceInputStream)
           (java.util Enumeration
                      NoSuchElementException)
           (clojure.lang SeqEnumeration))
  (:use [ring.util.response :only (response content-type)]))

(defn- get-param [request param]
  (or (get-in request [:params (keyword param)])
      (get-in request [:params (name    param)])))

(defn- json-content-type? [response]
  (re-matches #"application/json(;.*)?" (get-in response [:headers "Content-Type"] "")))

(defn- pad-json? [callback response]
  (and callback (json-content-type? response)))

(defn- seqable->enumeration
  [coll]
  (SeqEnumeration. (seq coll)))

(defn- string->stream [^String s]
  (ByteArrayInputStream. (.getBytes s)))

(defn- concat-streams
  [input-streams]
  (SequenceInputStream. (seqable->enumeration input-streams)))

(defn- body->stream [body]
  (cond (seq? body) (concat-streams
                     (for [x body] (string->stream (str x))))
        (instance? File body) (FileInputStream. body)
        (instance? InputStream body) body
        (string? body) (string->stream body)
        (nil? body) (string->stream "")
        ;; what the heck else can we do here? Should we make a protocol?
        :else (throw (Exception. (str "Don't know how to convert "
                                      (type body)
                                      " to an InputStream!")))))

(defn- add-padding-to-json [callback response]
  (-> response
      (content-type "application/javascript")
      (update-in [:body]
                 #(concat-streams
                   [(string->stream (str callback "("))
                    (body->stream %)
                    (string->stream ");")]))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [callback (get-param request :callback)
          response (handler request)]
      (if (pad-json? callback response)
          (add-padding-to-json callback response)
          response))))

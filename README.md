# ring.middleware.jsonp

## Usage

Wrap your [Ring][] application with `wrap-json-with-padding` and
JSON resources will automatically support JSONP.

Example:

```clojure
(def app
  (-> main-routes
      handler/site
      wrap-json-with-padding ; <--
      wrap-params))
```

## License

Copyright (C) 2012 [Christoffer Sawicki](mailto:christoffer.sawicki@gmail.com)

Distributed under the Eclipse Public License, the same as Clojure.

[Ring]: https://github.com/ring-clojure/ring

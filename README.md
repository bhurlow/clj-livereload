# Clj-livereload

[![Clojars Project](http://clojars.org/clj-livereload/latest-version.svg)](http://clojars.org/clj-livereload)

[LiveReload](http://livereload.com/) server implementation in Clojure.
Useful with [Chrome plugin](https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei).

## Try

```bash
$ lein start-example
# Edit files inside example-resources using your favorite editor:
$ vim example-resources/public/style.css
```

## Use

For [Boot](http://boot-clj.com) check [boot-livereload task](https://github.com/Deraen/boot-livereload).

Check [example](./example/clj_livereload/test.clj). It is also possible to use
`clj-livereload.core` namespace directly and maintain the global state yourself.


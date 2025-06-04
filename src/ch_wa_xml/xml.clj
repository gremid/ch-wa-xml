(ns ch-wa-xml.xml
  (:import
   (java.io OutputStream StringWriter Writer)
   (javax.xml XMLConstants)
   (javax.xml.stream XMLOutputFactory XMLStreamWriter)
   (javax.xml.transform Result)
   (javax.xml.transform.stream StreamResult)
   (org.codehaus.stax2 XMLOutputFactory2 XMLStreamProperties)))

(def ^XMLOutputFactory output-factory
  (doto (XMLOutputFactory2/newInstance)
    (.setProperty XMLStreamProperties/XSP_NAMESPACE_AWARE Boolean/FALSE)))

(defprotocol Output
  (as-result [v])
  (write-events [v event-writer]))

(def prefix
  XMLConstants/DEFAULT_NS_PREFIX)

(def xml-ns
  XMLConstants/NULL_NS_URI)

(extend-protocol Output
  Result
  (as-result [^Result v] v)

  OutputStream
  (as-result [^OutputStream v] (StreamResult. v))

  Writer
  (as-result [^Writer v] (StreamResult. v))

  clojure.lang.IPersistentMap
  (write-events [m ^XMLStreamWriter w]
    (let [ln (name (:tag m))]
      (.writeStartElement w xml-ns ln)
      (doseq [[k v] (:attrs m)]
        (.writeAttribute w xml-ns (name k) (str v)))
      (doseq [child (:content m)]
        (write-events child w))
      (.writeEndElement w)))

  String
  (write-events [^String s ^XMLStreamWriter w]
    (.writeCharacters w s)))

(defn stream-writer
  ([output]
   (stream-writer output-factory output))
  ([^XMLOutputFactory factory output]
   (.createXMLStreamWriter factory ^Result (as-result output))))

(defn emit
  [node target]
  (let [^XMLStreamWriter sw (stream-writer target)]
    (.writeStartDocument sw)
    (write-events node sw)
    (.writeEndDocument sw )))

(defn emit-str
  [node]
  (let [sw (StringWriter.)]
    (emit node sw)
    (str sw)))

(comment
  (emit-str {:tag     :b:a
             :attrs   {:c "xyz"}
             :content ["ab" {:tag :c} "xz"]}))

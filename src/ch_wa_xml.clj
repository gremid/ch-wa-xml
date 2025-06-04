(ns ch-wa-xml
  "Read Electronic Book Technologies' DynaText Persistent DOM files and output
  their data in XML format."
  (:require
   [ch-wa-xml.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [helins.binf :as binf]
   [helins.binf.buffer :as binf.buffer]
   [helins.binf.string :as binf.string]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]
   [taoensso.timbre :as log])
  (:import
   (java.io ByteArrayOutputStream)
   (java.nio ByteBuffer)))

(log/merge-config!
 {:appenders {:println (log/println-appender {:stream :std-err})}})

;; DynaText/SGML elements have attributes with slightly different notation than
;; their XML counterpart. Especially the quoting of values is more lenient.

(def attr-whitespace
  " \t\n\r")

(def +attr-ws
  (p/+many (char/is attr-whitespace)))

(def attr-value-unquoted
  (-> (p/*many (char/is-not attr-whitespace))
      (p/value char/str*)))

(def attr-value-double-quoted
  (->
   (p/group (char/is "\"") (p/*many (char/is-not "\"")) (char/is "\""))
   (p/value (fn [[_ v _]] (char/str* v)))))

(def attr-value-single-quoted
  (->
   (p/group (char/is "'") (p/*many (char/is-not "'")) (char/is "'"))
   (p/value (fn [[_ v _]] (char/str* v)))))

(def attr-value
  (p/alt attr-value-single-quoted
         attr-value-double-quoted
         attr-value-unquoted))

(def attr-name
  (->
   (p/+many (char/is-not "="))
   (p/value char/str*)))

(def attr
  (->
   (p/group attr-name (char/is "=") attr-value)
   (p/value (fn [[k _ v]] [(str/lower-case k) v]))))

(def attrs
  (->
   (p/group
    (p/option attr)
    (p/*many (-> (p/group +attr-ws attr) (p/value (fn [[_ attr]] attr)))))
   (p/value (fn [[attr attrs]] (when attr (cons attr attrs))))))

(defn parse-attrs
  [s]
  (into {} (p/parse attrs (str/trim s))))

(defn parse-record
  [{:keys [element? text? tag text] :as record}]
  (let [context (reverse (map str/lower-case (str/split tag #",")))
        context (cond->> context text? (rest))]
    (cond-> record
      :always  (dissoc :tag)
      :always  (assoc :context context)
      element? (assoc :tag (first context) :attrs (parse-attrs text)))))

;; See https://patents.google.com/patent/US6101512A/en?oq=6%2c101%2c512 for some
;; background information about DynaText's binary format representing on-disk
;; DOMs.

(def text-decoder
  (binf.string/decoder "windows-1252"))

(def header-length
  144)

(def record-length
  24)

(defn ra-u24
  [view]
  (let [temp-view (binf/view (binf.buffer/alloc 4))]
    (binf/wr-b8 temp-view 0)
    (binf/wr-b8 temp-view (binf/rr-u8 view))
    (binf/wr-b8 temp-view (binf/rr-u8 view))
    (binf/wr-b8 temp-view (binf/rr-u8 view))
    (binf/ra-u32 temp-view 0)))

(defn read-record
  [{:keys [directory tags text]} n]
  (let [offset       (+ header-length (* record-length n))
        record-view  (binf/view directory offset record-length)
        next-sibling (binf/ra-u32 record-view 0)
        text-start   (binf/ra-u32 record-view 4)
        flags        (binf/ra-u8 record-view 8)
        tag-start    (ra-u24 (binf/view record-view 9))
        tag-num      (binf/ra-u32 record-view 12)
        prev-sibling (binf/ra-u16 record-view 16)
        parent       (binf/ra-u16 record-view 18)
        text-length  (binf/ra-u16 record-view 20)
        tag-length   (binf/ra-u16 record-view 22)]
    (parse-record
     {:n            n
      :flags        (apply str (map #(if (bit-test flags %) "1" "0") (range 8)))
      :text?        (bit-test flags 2)
      :element?     (or (bit-test flags 1) (bit-test flags 6))
      :empty?       (bit-test flags 6)
      :last-child?  (bit-test flags 7)
      :next-sibling (when-not (zero? next-sibling) (dec next-sibling))
      :tag-num      tag-num
      :parent       (when-not (zero? parent) (- n parent))
      :prev-sibling (when-not (zero? prev-sibling) (- n prev-sibling))
      :text         (binf/ra-string text text-decoder text-start text-length)
      :tag          (binf/ra-string tags tag-start tag-length)})))

;; DynaText DOM to XML conversion

(declare node->xml)

(defn siblings->xml
  [data {:keys [next-sibling last-child?] :as sibling}]
  (lazy-cat [(node->xml data sibling)]
            (when-not last-child?
              (siblings->xml data (read-record data next-sibling)))))

(defn node->xml
  [{:keys [records] :as data} {:keys [element? n tag attrs text]}]
  (if element?
    (let [tag   (-> tag (str/replace #"[^a-z09]" "") keyword)
          attrs attrs]
      (when (= :doc tag)
        (log/infof "%s" attrs))
      {:tag     tag
       :attrs   attrs
       :content (when (< n (dec records))
                  (let [first-child (read-record data (inc n))]
                    (when (= n (first-child :parent))
                      (siblings->xml data first-child))))})
    (str/replace text #"[\r\n]+" " ")))

(defn file->buf
  [& args]
  (let [output (ByteArrayOutputStream.)]
    (with-open [input (io/input-stream (apply io/file args))]
      (io/copy input output))
    (ByteBuffer/wrap (.toByteArray ^ByteArrayOutputStream output))))

(defn -main
  ([]
   (-main "data"))
  ([base-dir]
   (let [data-dir  (io/file base-dir "goethe" "ebt")
         directory (file->buf (io/file data-dir "goethe.edr"))
         data      {:directory directory
                    :tags      (file->buf data-dir "goethe.tag")
                    :text      (file->buf data-dir "goethe.dat")
                    :records   (/ (- (count (binf/backing-buffer directory))
                                     header-length)
                                  record-length)}
         doc       (node->xml data (read-record data 1))]
     (with-open [os (io/output-stream (io/file base-dir "ch-wa.xml"))]
       (xml/emit doc os)))))

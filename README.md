# spring-boot-streaming-download-service
### download-using-streaming-response-body
An example for streaming large files in chunks using StreamingResponseBody in Spring Boot.

The API sits behind HTTP Basic (`admin` / `admin` by default, see
`application.yml`) and reads/writes the directory configured under
`file.directory`. The upload page itself is public.

## Browser UI

Open <http://localhost:8080/> — a single static page (`src/main/resources/static/index.html`,
no build step, no dependencies) with drag-and-drop, multi-file select, a live
progress bar and a switch between the two upload modes.

The page loads without a login and has its own **username / password** fields;
they are sent as an HTTP Basic header with every upload, so no browser
credential dialog appears. A rejected login is reported inline instead. The
username is remembered in `localStorage`, the password never is.

The zip link is an exception — a plain link cannot carry a header, so clicking
it does raise the usual browser prompt.

## Endpoints

| Method | Path | Body | Purpose |
|---|---|---|---|
| `GET`  | `/api/download` | — | Zips `file.directory` and streams it back |
| `POST` | `/api/upload` | `multipart/form-data`, field `files` | Zips one or many files into a single archive |
| `PUT`  | `/api/upload/stream?name=<fileName>` | raw bytes | Compresses the request body into an archive |

An upload is never stored loose: it is written as
`sample_<millis>.zip` — the same name shape `/api/download` produces, prefix
configurable via `file.zip-prefix` — with the original files as entries.

### Upload — multipart

```bash
curl -u admin:admin -F "files=@report.pdf" -F "files=@photo.jpg" \
     http://localhost:8080/api/upload
```

```json
{
  "archive": "sample_1788287531099.zip",
  "archiveSize": 301884,
  "count": 2,
  "files": [
    { "fileName": "report.pdf", "size": 254112 },
    { "fileName": "photo.jpg",  "size": 88190 }
  ]
}
```

`size` is the original, uncompressed size of each entry; `archiveSize` is the
zip on disk.

### Upload — raw stream (large files)

The request body is compressed into the archive in 4 KB chunks, so nothing is
buffered in memory. This is the upload mirror of the `StreamingResponseBody`
download.

```bash
curl -u admin:admin -X PUT --upload-file big.iso \
     "http://localhost:8080/api/upload/stream?name=big.iso"
```

```json
{
  "archive": "sample_1788287602431.zip",
  "archiveSize": 918273645,
  "count": 1,
  "files": [ { "fileName": "big.iso", "size": 1073741824 } ]
}
```

## Notes

* Archive names carry a millisecond timestamp, so an upload never overwrites an
  earlier one.
* Two files of the same name in one request do not collide — the second entry
  becomes `report_1.pdf`.
* Client supplied names are reduced to a bare file name, so `../` can neither
  escape the directory nor plant a path inside the archive — such a request is
  rejected with `400`.
* A failed upload deletes its half-written archive, so `/api/download` never
  picks up a corrupt zip.
* Multipart limits live in `application.yml`
  (`spring.servlet.multipart.max-file-size` / `max-request-size`, 2 GB each). The
  raw-stream endpoint is not bound by them.
* CSRF is disabled for `/api/**` only — without that, Spring Security's default
  would reject every `POST`/`PUT` from a non-browser client with a `403`.

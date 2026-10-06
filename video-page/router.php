<?php
// Local equivalent of the supplied Apache /file and /folder rewrites.
$path=parse_url($_SERVER['REQUEST_URI'],PHP_URL_PATH);
if (preg_match('#^/file/[a-zA-Z0-9_-]+/?$#',$path)) {require __DIR__.'/file.php';return true;}
if (preg_match('#^/folder/[a-zA-Z0-9_-]+/?$#',$path)) {require __DIR__.'/folder.php';return true;}
if ($path==='/config.php' || preg_match('/\.(env|sql|sqlite|md)$/i',$path)) {http_response_code(403);return true;}
return false;

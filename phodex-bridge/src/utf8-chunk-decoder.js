// FILE: utf8-chunk-decoder.js
// Purpose: Preserves partial multi-byte UTF-8 characters across chunk boundaries.
// Layer: Bridge helper
// Exports: createUtf8ChunkDecoder
// Depends on: node:string_decoder

const { StringDecoder } = require("node:string_decoder");

function createUtf8ChunkDecoder() {
  const decoder = new StringDecoder("utf8");

  return {
    write(chunk) {
      if (chunk == null) {
        return "";
      }
      if (typeof chunk === "string") {
        return chunk;
      }
      return decoder.write(chunk);
    },
    end(chunk) {
      if (chunk == null) {
        return decoder.end();
      }
      if (typeof chunk === "string") {
        return chunk + decoder.end();
      }
      return decoder.end(chunk);
    },
  };
}

module.exports = {
  createUtf8ChunkDecoder,
};

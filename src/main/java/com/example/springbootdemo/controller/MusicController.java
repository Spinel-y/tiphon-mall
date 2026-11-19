package com.example.springbootdemo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/music")
public class MusicController {

    private static final Path MUSIC_DIR = Paths.get("music").toAbsolutePath().normalize();

    @GetMapping("/list")
    public List<String> listSongs() throws IOException {
        if (!Files.exists(MUSIC_DIR)) {
            Files.createDirectories(MUSIC_DIR);
        }
        try (Stream<Path> files = Files.list(MUSIC_DIR)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.toLowerCase().endsWith(".mp3"))
                    .sorted()
                    .toList();
        }
    }

    @GetMapping("/play/{filename}")
    public void playSong(@PathVariable String filename,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        Path songPath = MUSIC_DIR.resolve(filename).normalize();
        if (!songPath.startsWith(MUSIC_DIR)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!Files.exists(songPath) || !Files.isRegularFile(songPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileSize = Files.size(songPath);
        response.setContentType("audio/mpeg");
        response.setHeader("Accept-Ranges", "bytes");

        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            long[] range = parseRange(rangeHeader, fileSize);
            if (range == null) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileSize);
                return;
            }
            long start = range[0], end = range[1];
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
            response.setContentLengthLong(end - start + 1);

            try (var in = Files.newInputStream(songPath);
                 OutputStream out = response.getOutputStream()) {
                long skipped = 0;
                while (skipped < start) {
                    long s = in.skip(start - skipped);
                    if (s <= 0) break;
                    skipped += s;
                }
                byte[] buf = new byte[8192];
                long remaining = end - start + 1;
                int read;
                while (remaining > 0 && (read = in.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                    out.write(buf, 0, read);
                    remaining -= read;
                }
            }
        } else {
            response.setContentLengthLong(fileSize);
            Files.copy(songPath, response.getOutputStream());
        }
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        String rangeValue = rangeHeader.substring("bytes=".length());
        int dashIdx = rangeValue.indexOf('-');
        long start = 0, end = fileSize - 1;
        try {
            if (dashIdx > 0) start = Long.parseLong(rangeValue.substring(0, dashIdx));
            if (dashIdx < rangeValue.length() - 1) end = Long.parseLong(rangeValue.substring(dashIdx + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (start >= fileSize) return null;
        if (end >= fileSize) end = fileSize - 1;
        return new long[]{start, end};
    }
}

package fr.tikione.steam.cleaner.util;

import static fr.tikione.steam.cleaner.util.FileUtils.checkFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class performs multi-threaded analysis on files.
 */
public class ThreadedFileComparator {

    private final List<File> files;

    private final List<Redist> redistsPatterns;

    private final List<Redist> checkedFiles;

    private final boolean onFiles;

    public ThreadedFileComparator(List<File> files, List<Redist> redistsPatterns, List<Redist> checkedFiles, boolean onFiles) {
        this.files = Collections.unmodifiableList(files);
        this.redistsPatterns = Collections.unmodifiableList(redistsPatterns);
        this.checkedFiles = checkedFiles;
        this.onFiles = onFiles;
    }

    /**
     * Perform multi-threaded analysis.
     *
     * @throws InterruptedException if an errro occurs on threads synchronization.
     */
    public final void start()
            throws InterruptedException {
        int nbfiles = files.size();
        if (nbfiles > 0) {
            int nbthreads = 4;
            int segment;
            if (nbfiles > nbthreads) {
                segment = nbfiles / nbthreads;
            } else {
                segment = nbfiles;
                nbthreads = 1;
            }
            List<Thread> threads = new ArrayList<>(nbthreads);
            for (int nt = 0; nt < nbthreads; nt++) {
                int startIdx = nt * segment;
                int endIdx = (nt == nbthreads - 1) ? nbfiles - 1 : startIdx + segment - 1;
                FileComparatorWorker swThread = new FileComparatorWorker(files, startIdx, endIdx, redistsPatterns, onFiles);
                swThread.start();
                threads.add(swThread);
            }
            for (Thread thread : threads) {
                thread.join();
                List<Redist> res = ((FileComparatorWorker) thread).checkedFiles;
                checkedFiles.addAll(res);
            }
        }
    }

    class FileComparatorWorker extends Thread {

        /** List of files to exam (warning: this list in unmodifiable). */
        private final List<File> files;

        private final int startIdx;

        private final int endIdx;

        private final List<Redist> redistsPatterns;

        public final List<Redist> checkedFiles;

        private boolean onFiles;

        FileComparatorWorker(List<File> files, int startIdx, int endIdx, List<Redist> redistsPatterns, boolean onFiles) {
            this.files = files;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.redistsPatterns = redistsPatterns;
            this.checkedFiles = new ArrayList<>(20);
            this.onFiles = onFiles;
        }

        @Override
        public void run() {
            for (int pos = startIdx; pos <= endIdx; pos++) {
                Redist candidate = checkFile(files.get(pos), redistsPatterns);
                if (candidate != null) {
                    if (onFiles ? candidate.getFile().isFile() : candidate.getFile().isDirectory()) {
                        if (!checkedFiles.contains(candidate)) {
                            checkedFiles.add(candidate);
                        }
                    }
                }
            }
        }
    }
}

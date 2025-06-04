package org.j0schi.services;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitService {

    public boolean pullRepository(String repoPath) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            System.out.printf("Pulling changes from repository: %s%n", repoPath);
            PullResult result = git.pull().setRebase(true).call();
            System.out.println("Pull result: " + result);
            return result.isSuccessful();
        } catch (GitAPIException | IOException e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    public boolean commitAndPush(String repoPath, String commitMessage) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            // Проверяем есть ли изменения
            if (git.status().call().isClean()) {
                System.out.println("No changes to commit in: " + repoPath);
                return true;
            }

            System.out.println("Committing changes: " + commitMessage);
            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();

            System.out.println("Pushing changes...");
            git.push().call();
            return true;
        } catch (GitAPIException | IOException e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    private Git openRepository(String repoPath) throws IOException {
        Path gitDir = Path.of(repoPath, ".git");
        if (!Files.exists(gitDir)) {
            System.err.println("ERROR: Not a Git repository (missing .git folder): " + repoPath);
            return null;
        }

        Repository repository = new FileRepositoryBuilder()
                .setGitDir(gitDir.toFile())
                .build();

        if (repository.getRemoteNames().isEmpty()) {
            System.err.println("WARNING: No remote origin configured for repository: " + repoPath);
        }

        return new Git(repository);
    }

    private void handleGitError(String repoPath, Exception e) {
        System.err.println("Git operation failed for: " + repoPath);
        System.err.println("Error: " + e.getMessage());

        if (e.getCause() != null) {
            System.err.println("Cause: " + e.getCause().getMessage());
        }
    }
}

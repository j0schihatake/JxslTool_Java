package org.j0schi.services;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GitService {

    private String username = "vdonskoy";

    private String password = "Q-5x+Ies";

    private final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);

    public boolean pullRepository(String repoPath) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            // Проверяем наличие удаленных репозиториев
            if (!hasRemotes(git)) {
                System.out.println("WARNING: Skipping pull - no remotes configured for repository: " + repoPath);
                return true;
            }

            System.out.printf("Pulling changes from repository: %s%n", repoPath);

            // Пытаемся сделать pull с upstream, если он есть
            String upstreamBranch = getUpstreamBranch(git);

            PullCommand pullCommand = git.pull();
            if (credentialsProvider != null) {
                pullCommand.setCredentialsProvider(credentialsProvider);
            }

            if (upstreamBranch != null) {
                pullCommand.setRemoteBranchName(upstreamBranch);
                System.out.println("Using upstream branch: " + upstreamBranch);
            }

            PullResult result = pullCommand.setRebase(true).call();
            System.out.println("Pull result: " + result);
            return result.isSuccessful();
        } catch (GitAPIException | IOException e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    public boolean commitAndPush(String repoPath, String commitMessage) {
        return commitAndPush(repoPath, commitMessage, ".");
    }

    public boolean commitAndPush(String repoPath, String commitMessage, String pathToAdd) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            // Проверяем есть ли изменения
            if (git.status().call().isClean()) {
                System.out.println("No changes to commit in: " + repoPath);
                return true;
            }

            System.out.println("Committing changes: " + commitMessage);
            // Добавляем ТОЛЬКО указанную директорию:
            git.add().addFilepattern(pathToAdd).call();
            git.commit().setMessage(commitMessage).call();

            // Проверяем наличие удаленных репозиториев
            if (!hasRemotes(git)) {
                System.out.println("WARNING: Skipping push - no remotes configured for repository: " + repoPath);
                return true;
            }

            System.out.println("Pushing changes...");
            PushCommand pushCommand = git.push();
            if (credentialsProvider != null) {
                pushCommand.setCredentialsProvider(credentialsProvider);
            }

            // Если есть upstream, пушим в него
            String upstreamBranch = getUpstreamBranch(git);
            if (upstreamBranch != null) {
                pushCommand.setRemote("upstream");
                System.out.println("Pushing to upstream branch");
            }

            pushCommand.call();
            return true;
        } catch (GitAPIException | IOException e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    public boolean syncWithRemote(String repoPath) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            // Проверяем наличие удаленных репозиториев
            if (!hasRemotes(git)) {
                System.out.println("WARNING: Skipping sync - no remotes configured for repository: " + repoPath);
                return true;
            }

            System.out.printf("Synchronizing repository: %s%n", repoPath);

            // 1. Pull последних изменений
            if (!pullRepository(repoPath)) {
                return false;
            }

            // 2. Push изменений
            return commitAndPush(repoPath, "Automatic synchronization");
        } catch (Exception e) {
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

        return new Git(repository);
    }

    private boolean hasRemotes(Git git) {
        return git != null &&
                git.getRepository() != null &&
                !git.getRepository().getRemoteNames().isEmpty();
    }

    private String getUpstreamBranch(Git git) throws IOException {
        if (git == null) return null;

        String currentBranch = git.getRepository().getBranch();
        if (currentBranch == null) return null;

        // Получаем upstream для текущей ветки
        Ref upstreamRef = git.getRepository().findRef(currentBranch + "@{upstream}");
        return upstreamRef != null ? upstreamRef.getName() : null;
    }

    private void handleGitError(String repoPath, Exception e) {
        System.err.println("Git operation failed for: " + repoPath);
        System.err.println("Error: " + e.getMessage());

        if (e.getCause() != null) {
            System.err.println("Cause: " + e.getCause().getMessage());
        }

        // Логируем stack trace для отладки
        e.printStackTrace();
    }

    public boolean isRepository(String repoPath) {
        return Files.exists(Paths.get(repoPath, ".git"));
    }

    public String getUpstreamBranch(String repoPath) throws IOException {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return null;

            String currentBranch = git.getRepository().getBranch();
            if (currentBranch == null) return null;

            Ref upstreamRef = git.getRepository().findRef(currentBranch + "@{upstream}");
            return upstreamRef != null ? upstreamRef.getName() : null;
        }
    }

    public boolean hasRemote(String repoPath, String remoteName) throws IOException {
        try (Git git = openRepository(repoPath)) {
            return git != null && git.getRepository().getRemoteNames().contains(remoteName);
        }
    }

    public boolean pushToRemote(String repoPath, String remoteName) throws GitAPIException, IOException {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            PushCommand pushCommand = git.push().setRemote(remoteName);
            Iterable<PushResult> results = pushCommand.call();

            boolean success = true;
            for (PushResult result : results) {
                System.out.println("Push result to " + remoteName + ": " + result.getMessages());
                success &= result.getRemoteUpdates().stream()
                        .allMatch(update -> update.getStatus() == RemoteRefUpdate.Status.OK);
            }
            return success;
        }
    }

    public boolean pullFromRemote(String repoPath, String remoteName) throws GitAPIException, IOException {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;

            PullResult result = git.pull()
                    .setRemote(remoteName)
                    .setRebase(true)
                    .call();

            System.out.println("Pull result from " + remoteName + ": " + result);
            return result.isSuccessful();
        }
    }

    public boolean hasChanges(String repoPath) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;
            return !git.status().call().isClean();
        } catch (Exception e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    public boolean commit(String repoPath, String commitMessage) {
        try (Git git = openRepository(repoPath)) {
            if (git == null) return false;
            if (git.status().call().isClean()) {
                System.out.println("No changes to commit in: " + repoPath);
                return true;
            }

            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();
            return true;
        } catch (Exception e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    public boolean hasRemotes(String repoPath) {
        try (Git git = openRepository(repoPath)) {
            return git != null && !git.getRepository().getRemoteNames().isEmpty();
        } catch (Exception e) {
            handleGitError(repoPath, e);
            return false;
        }
    }

    /**
     * Вычислить относительный путь:
     * @param codGitPath
     * @param codPath
     * @return
     */
    public String getRelativeCodPath(String codGitPath, String codPath) {
        Path repoPath = Paths.get(codGitPath);
        Path targetPath = Paths.get(codPath);
        return repoPath.relativize(targetPath).toString();
    }
}
import os
from github import Github, GithubException
from github.GithubException import *
from documentprojection.utils.logging import get_log
log = get_log(__name__)

class GithubPR():
    def __init__(self,
                 repo_name,
                 repo_branch_name = "master",
                 branch_name = "update-docs",
                 title = "Update Docs",
                 body = "Update documentation.",
                 token_name = "GH_TOKEN",
                 ) -> None:
        token = os.environ.get(token_name)
        if token is None:
            raise ValueError(f"Github access token {token_name} not found in environment variables.")
        
        self.client = Github(token)
        self.repo_branch_name = repo_branch_name
        self.title = title
        self.body = body
        self.branch_name = branch_name
        self.repo_name = repo_name
        self.files = []
    
    def add_file(self, file_path, file_content):
        self.files.append((file_path, file_content))
        return self
    
    def create(self):
        repo = self.client.get_repo(self.repo_name)
        repo.create_git_ref(ref="refs/heads/" + self.branch_name, sha=repo.get_branch(self.repo_branch_name).commit.sha)

        for file in self.files:
            repo.create_file(file[0], "Adding new file", file[1], branch=self.branch_name)

        pull_request = repo.create_pull(title=self.title, body=self.body, head=self.branch_name, base=self.repo_branch_name)
        log.info(f"Pull request for {self.repo_name} created successfully: {pull_request.html_url}")

    def try_create(self) -> bool:
        try:
            self.create()
            return True
        except Exception as e:
            log.error(f"Failed to create pull request for repo {self.repo_name}: {e}")
            return False

def do_stuff():
    GithubPR().add_file("test.txt", "New file content3").create()

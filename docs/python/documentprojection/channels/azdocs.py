from documentprojection.framework import *
from documentprojection.framework.git import GithubPR
from documentprojection.utils.logging import get_log
log = get_log(__name__)

class AzureDocsPR(GithubPR):
    def __init__(self,
                 branch_name = "update-docs",
                 title = "Update Docs",
                 body = "Update documentation."):
        azure_docs_repo_name = "MicrosoftDocs/azure-docs"
        azure_docs_repo_branch_name = "main"
        super().__init__(repo_name=azure_docs_repo_name,
                         repo_branch_name=azure_docs_repo_branch_name,
                         branch_name=branch_name,
                         title=title,
                         body=body)

class AzureDocsDoc(Document):
    def __init__(self, content, metadata):
        self.content = content
        self.metadata = metadata
        log.error(f"{self.__qualname__} NOT IMPLEMENTED")

class AzureDocsFormatter(Formatter):
    def format(self, notebook: Notebook) -> Document:
        log.error(f"{self.format.__qualname__} NOT IMPLEMENTED")
        return Document(notebook.data, self.get_metadata(notebook))
    
    def get_metadata(self, notebook: Notebook) -> DocumentMetadata:
        log.error(f"{self.get_metadata.__qualname__} NOT IMPLEMENTED")
        return DocumentMetadata(notebook.path, "")
    
class AzureDocsPublisher(Publisher):
    def publish(self, document: Document) -> bool:
        log.error(f"{self.publish.__qualname__} NOT IMPLEMENTED")
        pr = AzureDocsPR()
        return True

class AzureDocsChannel(Channel):
    def __init__(self):
        self.formatter = AzureDocsFormatter()
        self.publisher = AzureDocsPublisher()
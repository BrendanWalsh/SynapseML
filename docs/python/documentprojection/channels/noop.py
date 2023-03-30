from documentprojection.framework import *
from documentprojection.utils.logging import get_log
log = get_log(__name__)

class NoopDoc(Document):
    def __init__(self, content, metadata):
        self.content = content
        self.metadata = metadata

class NoopFormatter(Formatter):
    def format(self, notebook: Notebook) -> Document:
        log.error(f"{self.format.__qualname__} NOT IMPLEMENTED")
        return Document(notebook.data, self.get_metadata(notebook))
    
    def get_metadata(self, notebook: Notebook) -> DocumentMetadata:
        log.error(f"{self.get_metadata.__qualname__} NOT IMPLEMENTED")
        return DocumentMetadata(notebook.path, "")

class NoopPublisher(Publisher):
    def publish(self, document: Document) -> bool:
        log.error(f"{self.publish.__qualname__} NOT IMPLEMENTED")
        pass

class NoopChannel(Channel):
    def __init__(self):
        self.formatter = NoopFormatter()
        self.publisher = NoopPublisher()
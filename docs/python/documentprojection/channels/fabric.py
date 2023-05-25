from ..framework import *
from ..utils.logging import get_log
from ..framework.markdown import MarkdownFormatter

log = get_log(__name__)

# A sample Fabric (no-operation) channel that 'publishes' to the Fabric. Useful for testing.
class FabricDoc(Document):
    def __init__(self, content, metadata):
        self.content = content
        self.metadata = metadata


class FabricFormatter(MarkdownFormatter):
    def clean_markdown(self, markdown: str) -> str:
        return markdown

    def get_header(self, notebook: Notebook) -> str:
        return "This is a test header injected by the 'Fabric' formatter."

    def get_metadata(self, notebook: Notebook) -> dict:
        return {"source_path": notebook.path, "target_path": "stdout"}


class FabricPublisher(Publisher):
    def publish(self, document: Document) -> bool:
        print(document.content)
        return True


class FabricChannel(Channel):
    def __init__(self, _):
        self.formatter = FabricFormatter()
        self.publisher = FabricPublisher()

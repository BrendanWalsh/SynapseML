import os
import re

from documentprojection.framework import *
from documentprojection.utils.logging import get_log
from documentprojection.framework.markdown import MarkdownFormatter
from documentprojection.utils.reflection import get_project_root
log = get_log(__name__)

class WebsiteDoc(Document):
    def __init__(self, content, metadata):
        self.content = content
        self.metadata = metadata

class WebsiteFormatter(MarkdownFormatter):
    def clean_markdown(self, markdown: str) -> str:
        markdown = re.sub(r"style=\"[\S ]*?\"", "", markdown)
        markdown = re.sub(r"<style[\S \n.]*?</style>", "", markdown)
        return markdown
    
    def get_header(self, notebook: Notebook) -> str:
        filename = os.path.basename(notebook.path)
        # TODO: sidebar_label?
        return f"---\ntitle: {filename}\nhide_title: true\nstatus: stable\n---"

    def get_metadata(self, notebook: Notebook) -> DocumentMetadata:
        feature_dir = os.path.basename(os.path.dirname(notebook.path))
        website_path = os.path.join(get_project_root(), "website", "docs", "features", feature_dir, "about.md")
        return DocumentMetadata(notebook.path, website_path)

class WebsitePublisher(Publisher):
    def publish(self, document: Document) -> bool:
        log.info(document.metadata)
        log.info(document.content)
        # TODO: publish to website

class WebsiteChannel(Channel):
    def __init__(self):
        self.formatter = WebsiteFormatter()
        self.publisher = WebsitePublisher()

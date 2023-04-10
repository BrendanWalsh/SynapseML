from utils.reflection import get_subclasses
from framework import Tester

all_testers = get_subclasses(__name__, Tester)

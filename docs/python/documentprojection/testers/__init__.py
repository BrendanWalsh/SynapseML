from documentprojection.utils.reflection import get_subclasses
from documentprojection.framework import Tester

all_testers = get_subclasses(__name__, Tester)
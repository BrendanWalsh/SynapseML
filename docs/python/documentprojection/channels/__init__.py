from documentprojection.utils.reflection import get_subclasses
from documentprojection.framework import Channel

all_channels = get_subclasses(__name__, Channel)

from .console import *
from .noop import *
from .azdocs import *
from .website import *
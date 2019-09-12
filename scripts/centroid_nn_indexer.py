import sqlite3
from os.path import expanduser
import functools
from array import array

home = expanduser("~")


conn = sqlite3.connect(db_file, check_same_thread=False)


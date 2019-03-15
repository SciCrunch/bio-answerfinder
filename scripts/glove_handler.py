import sqlite3
# import pickle
from os.path import expanduser
import functools
from array import array


def populate_glove_db(glove_text_file, db_file):
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    cursor.execute('''create table if not exists glove_vecs (term text not null
                   primary key, vector blob)''')
    cursor.execute("PRAGMA synchronous = OFF")
    cursor.execute("PRAGMA journal_mode = MEMORY")
    conn.commit()
    query = u'''insert into glove_vecs values(?,?)'''
    count = 0
    with open(glove_text_file) as f:
        for line in f:
            tokens = line.split(' ')
            term = tokens[0]
            glove_vec = [float(x) for x in tokens[1:]]
            a = array('f', glove_vec)
            # b = sqlite3.Binary(pickle.dumps(glove_vec))
            b = sqlite3.Binary(a)

            cursor.execute(query, (term, b))
            count += 1
            if (count % 100) == 0:
                conn.commit()
            # if count > 200:
            #    break
    conn.commit()

    cursor.close()
    conn.close()


class GloveHandler:
    def __init__(self, db_file):
        self.conn = sqlite3.connect(db_file, check_same_thread=False)
        cursor = self.conn.cursor()
        cursor.execute("PRAGMA synchronous = OFF")
        cursor.execute("PRAGMA journal_mode = MEMORY")
        self.conn.commit()
        cursor.close()


    def close(self):
        if self.conn:
            self.conn.close()

    @functools.lru_cache(maxsize=65536)
    def get_glove_vec(self, term):
        sql = "select vector from glove_vecs where term = :term"
        cursor = self.conn.cursor()
        cursor.execute(sql, {"term": term})
        vec_blob = cursor.fetchone()
        cursor.close()
        if vec_blob:
            arr = array('f')
            # glove_vec = pickle.loads(vec_blob[0])
            # print(vec_blob[0])
            arr.fromstring(vec_blob[0])
            glove_vec = arr.tolist()
        else:
            return None
        return glove_vec


if __name__ == "__main__":
    home = expanduser("~")
    glove_file = home + "/data/glove/pmc_2017_abstracts_glove_vectors_unk.txt"
    db_file = home + "/medline_glove_v2.db"
    populate_glove_db(glove_file, db_file)

    gh = GloveHandler(db_file)
    print(gh.get_glove_vec('was'))
    print(gh.get_glove_vec('is'))
    print(gh.get_glove_vec('burak'))
    gh.close()

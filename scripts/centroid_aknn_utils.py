import sqlite3
from struct import unpack
import numpy as np
import nmslib
import pickle
import time
from tqdm import tqdm


class CentroidKNN:
    def __init__(self, index_file, pmid_map_file):
        self.index = nmslib.init(method='hnsw', space='cosinesimil')
        self.index.loadIndex(index_file)
        self.pmid_map = pickle.load( open(pmid_map_file,"rb") )

    def query(self,query_vec, k=10):
        ids, distances = self.index.knnQuery(query_vec, k=k)
        print(ids)
        print(distances)
        pmids = [self.pmid_map[id] for id in ids]
        return pmids


class NMSLibIndexer:
    def __init__(self, in_db_file):
        self.con = sqlite3.connect(db_file, check_same_thread=False)
        cursor = self.con.cursor()
        cursor.execute("PRAGMA synchronous = OFF")
        cursor.execute("PRAGMA journal_mode = MEMORY")
        self.con.commit()
        cursor.close()

    def close(self):
        if self.con:
            self.con.close()

    def get_centroid_count(self, start_pmid=None):
        cursor = self.con.cursor()
        if start_pmid:
            sql = "select count(*) from centroids where pmid > :pmid"
            cursor.execute(sql, {"pmid": start_pmid})
        else:
            sql = "select count(*) from centroids where pmid > :pmid"
            cursor.execute(sql)
        count = int(cursor.fetchone()[0])
        cursor.close()
        return count

    def index(self, index_file, pmid_map_file, start_pmid=27000000):
        no_cv = self.get_centroid_count(start_pmid)
        print('no_cv:', no_cv)
        pmid_table = dict()
        cvm = np.ndarray((no_cv, 100), dtype='float32')
        sql = "select pmid, centroid from centroids where pmid > :pmid"
        cursor = self.con.cursor()
        cursor.execute(sql, {"pmid": start_pmid})

        i = 0
        for row in tqdm(cursor, desc='Loading centroids ',unit=' rows'):
            if row[1]:
                pmid = row[0]
                num_items = unpack('>h', row[1][:2])[0]
                buf = row[1][2:]
                cvm[i] = [unpack('>f',buf[i*4:i*4+4])[0] for i in range(num_items)]
                pmid_table[i] = pmid
                i += 1
        cursor.close()
        index = nmslib.init(method='hnsw', space='cosinesimil')
        index.addDataPointBatch(cvm)
        index.createIndex({'post': 2}, print_progress=True)
        index.saveIndex(index_file)
        pickle.dump(pmid_table, open(pmid_map_file, "wb"))
        return cvm[0]


def test_driver():
    db_file='/data/burak2_backup/data/medline_index/centroids.db'
    con = sqlite3.connect(db_file, check_same_thread=False)
    cursor = con.cursor()
    cursor.execute("PRAGMA synchronous = OFF")
    cursor.execute("PRAGMA journal_mode = MEMORY")
    con.commit()
    cursor.close()
    
    sql = "select pmid, centroid from centroids limit 5"
    cursor = con.cursor()
    cursor.execute(sql)
    for row in cursor:
        pmid = row[0]
        print(pmid)
        if row[1]:
            num_items = unpack('>h', row[1][:2])[0]
            buf = row[1][2:]
        cv = [unpack('>f',buf[i*4:i*4+4])[0] for i in range(num_items)] 
        # print(unpack('>h', row[1][:2]))
        print(cv)
    cursor.close()
    con.close()

def extract_pmids(db_file, out_file):
    con = sqlite3.connect(db_file, check_same_thread=False)
    cursor = con.cursor()
    cursor.execute("PRAGMA synchronous = OFF")
    cursor.execute("PRAGMA journal_mode = MEMORY")
    con.commit()
    cursor.close()
    
    sql = "select pmid from centroids"
    cursor = con.cursor()
    cursor.execute(sql)
    with open(out_file, 'w') as f:
        for row in cursor:
            f.write("%s\n" % row[0])
    cursor.close()

def test_index_query(db_file, index_file, pmid_map_file, start_pmid):
    indexer = NMSLibIndexer(db_file)
    try:
        start_time = time.time()
        test_query_vec = indexer.index(index_file, pmid_map_file, start_pmid)
        # test_query_vec = indexer.index(index_file, pmid_map_file, 27800000)
        elapsed_time = time.time() - start_time
        print("\nIndex time (secs):", elapsed_time)
        knn = CentroidKNN(index_file, pmid_map_file)

        pmids =  knn.query(test_query_vec)
        print("pmids:", pmids)
    finally:
        indexer.close()


if __name__ == '__main__':
    db_file = '/home/bozyurt/data/pubmed/centroids2019_baseline.db'
    #db_file='/data/burak2_backup/data/medline_index/centroids.db'
    index_file = '/tmp/centroid_idx.dat'
    pmid_map_file = '/tmp/pmid_map.p'
    # extract_pmids(db_file, '/tmp/centroid_pmids.txt')
    test_index_query(db_file, index_file, pmid_map_file, 15978865)





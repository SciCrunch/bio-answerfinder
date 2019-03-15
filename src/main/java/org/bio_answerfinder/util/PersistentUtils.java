package org.bio_answerfinder.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class PersistentUtils {

   private PersistentUtils() {
   }

   public static byte[] toBytes(Blob blob) throws Exception {
      ByteArrayOutputStream bout = null;
      InputStream in = null;
      try {
         in = blob.getBinaryStream();
         bout = new ByteArrayOutputStream();
         byte[] buf = new byte[256];
         int readBytes = 0;
         while ((readBytes = in.read(buf)) != -1) {
            bout.write(buf, 0, readBytes);
         }
         return bout.toByteArray();
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (Exception x) {
            }
         }
      }
   }

   public static void closeStatetement(PreparedStatement pst) {
      if (pst != null) {
         try {
            pst.close();
         } catch (Exception x) {
         }
      }
   }

   public static void closeStatetement(Statement st) {
      if (st != null) {
         try {
            st.close();
         } catch (Exception x) {
         }
      }
   }

   public static void closeResultSet(ResultSet rs) {
      if (rs != null) {
         try {
            rs.close();
         } catch (Exception x) {
         }
      }
   }

   public static int getNextVal(Connection con, String seqName)
         throws SQLException {
      Statement st = null;
      try {
         st = con.createStatement();
         ResultSet rs = st.executeQuery("select nextval('" + seqName + "')");
         int id = -1;
         if (rs.next()) {
            id = rs.getInt(1);
         }
         rs.close();
         return id;
      } finally {
         closeStatetement(st);
      }
   }

   public static String prepInQuery(String staticSQL, List<String> ids,
         String inVarname) {
      StringBuffer buf = new StringBuffer();
      buf.append(staticSQL);
      if (ids.size() == 1) {
         buf.append(" where ").append(inVarname).append(" = '");
         buf.append(ids.get(0)).append("'");
      } else {
         buf.append(" where ").append(inVarname).append(" in (");
         for (Iterator<String> iter = ids.iterator(); iter.hasNext();) {
            String id = iter.next();
            buf.append("'").append(id).append("'");
            if (iter.hasNext())
               buf.append(',');
         }
         buf.append(")");
      }

      return buf.toString();
   }

   public static String prepInQueryInt(String staticSQL, List<String> ids,
         String inVarname) {
      StringBuffer buf = new StringBuffer();
      buf.append(staticSQL);
      if (ids.size() == 1) {
         buf.append(" where ").append(inVarname).append(" = ");
         buf.append(ids.get(0));
      } else {
         buf.append(" where ").append(inVarname).append(" in (");
         for (Iterator<String> iter = ids.iterator(); iter.hasNext();) {
            String id = iter.next();
            buf.append(id);
            if (iter.hasNext())
               buf.append(',');
         }
         buf.append(")");
      }

      return buf.toString();
   }
}

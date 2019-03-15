package org.bio_answerfinder.common.types;

import java.io.Serializable;

/**
 * represents a range in a sequence of objects, including the start index and
 * excluding the end index.
 * 
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Range implements Serializable {
   private static final long serialVersionUID = 1L;
   protected int startIdx;
   protected int endIdx;

   public Range(int startIdx, int endIdx) {
      this.startIdx = startIdx;
      this.endIdx = endIdx;
   }

   public int getEndIdx() {
      return endIdx;
   }

   public int getStartIdx() {
      return startIdx;
   }

   public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof Range)) {
         return false;
      }
      Range r2 = (Range) obj;
      return (startIdx == r2.startIdx && endIdx == r2.endIdx);
   }
   

   public int hashCode() {
      StringBuilder buf = new StringBuilder();
      buf.append(startIdx).append('_').append(endIdx);
      return buf.toString().hashCode();
   }

   public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append("Range::[");
      buf.append("startIdx=").append(startIdx);
      buf.append(",endIdx=").append(endIdx);
      buf.append(']');
      return buf.toString();
   }
}// ;

/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.pdf;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ExportPdfParameters implements Serializable {
   private static final long serialVersionUID = 1L;

   public final long time;
   public final String exportFormatString;
   private final List<ExportDistiller.ExportEntry> active;
   private final List<ExportDistiller.ExportEntry> archived;
   private final List<ExportDistiller.ExportEntry> allEntries;

   public ExportPdfParameters(long time, String exportFormatString,
                              List<ExportDistiller.ExportEntry> active, List<ExportDistiller.ExportEntry> archived) {
      this.time = time;
      this.exportFormatString = exportFormatString;
      this.active = active;
      this.archived = archived;
      allEntries = new LinkedList<>();
      allEntries.addAll(active);
      allEntries.addAll(archived);
   }

   public List<ExportDistiller.ExportEntry> getAllEntries() {
      return Collections.unmodifiableList(allEntries);
   }

   public int getNumActive() {
      return active.size();
   }

   public int getNumArchived() {
      return archived.size();
   }

   public List<ExportDistiller.ExportEntry> getActive() {
      return Collections.unmodifiableList(active);
   }

   public List<ExportDistiller.ExportEntry> getArchived() {
      return Collections.unmodifiableList(archived);
   }

   public int entriesWithEncryptedKeys() {
      Iterable<ExportDistiller.ExportEntry> entries = Iterables.concat(active, archived);
      FluentIterable<ExportDistiller.ExportEntry> entryWithKey = FluentIterable.from(entries).filter(
            new Predicate<ExportDistiller.ExportEntry>() {
               @Override
               public boolean apply(ExportDistiller.ExportEntry input) {
                  return input.encryptedKey != null;
               }
            });
      return entryWithKey.size();
   }
}

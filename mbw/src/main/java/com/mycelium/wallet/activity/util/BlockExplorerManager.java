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

package com.mycelium.wallet.activity.util;

import com.google.common.base.Preconditions;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.MbwManager;


import java.util.List;

// BlockExplorerManager holds all BlockExplorers in List and return by Identifier
public class BlockExplorerManager {


   private final MbwManager mbwManager;
   private final List<BlockExplorer> blockExplorerList;
   private BlockExplorer currentBlockExplorer;

   public BlockExplorerManager(MbwManager mbwManager, List<BlockExplorer> blockExplorerList, String blockExplorer){
      this.mbwManager=mbwManager;
      this.blockExplorerList = blockExplorerList;
      this.currentBlockExplorer = getBlockExplorerById(blockExplorer);
   }


   public BlockExplorer getBlockExplorerById(String id) {
      for (BlockExplorer explorer : blockExplorerList) {
         if (explorer.getIdentifier().equals(id)) return explorer;
      }
      // throw new RuntimeException("No matching block explorer found: " + name);
      return blockExplorerList.get(0);
   }

   public List<BlockExplorer> getAllBlockExplorer(){
      return blockExplorerList;
   }

   public BlockExplorer getBlockExplorer (){
      Preconditions.checkNotNull(currentBlockExplorer);
      // if tor mode is on and the current BE has no tor url, find the next best one
      if(mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR && !currentBlockExplorer.hasTor()){
         for(BlockExplorer explorer : blockExplorerList){
            if(explorer.hasTor()) {
               return explorer;
            }
         }
      }
      return currentBlockExplorer;
   }

   public CharSequence[] getBlockExplorerIds(){
      CharSequence[] ids = new CharSequence[blockExplorerList.size()];
      int index = 0;
      for (BlockExplorer explorer : blockExplorerList) {
         ids[index] = explorer.getIdentifier();
         index++;
      }
      return ids;
   }

   public CharSequence[] getBlockExplorerNames(List<BlockExplorer> blockList){
      CharSequence[] names = new CharSequence[blockList.size()];
      int index = 0;
      for (BlockExplorer explorer : blockList) {
         names[index] = explorer.getTitle();
         index++;
      }
      return names;
   }


   public void setBlockExplorer(BlockExplorer blockExplorer) {
      currentBlockExplorer = blockExplorer;
   }
}

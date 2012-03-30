/*
 * Copyright 2012 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.twitter.cassovary.graph

import it.unimi.dsi.fastutil.{Arrays, Swapper}
import it.unimi.dsi.fastutil.objects.{Object2IntOpenHashMap, Object2IntMaps}
import it.unimi.dsi.fastutil.ints.{Int2ObjectOpenHashMap, IntComparator}


/**
 * Represents a collection of directed paths. For example, a collection of paths out of
 * the same source node. This class is *not* thread safe!
 */
class DirectedPathCollection {

  // the current path being built
  private val currPath = DirectedPath.builder()

  // pathCountsPerId(id)(path) = count of #times path has been seen ending in id
  private val pathCountsPerId = new Int2ObjectOpenHashMap[Object2IntOpenHashMap[DirectedPath]]

  /**
   * Appends node to the current path and record this path against the node.
   * @param node the node being visited
   */
  def appendToCurrentPath(node: Int) {
    currPath.append(node)
    pathCountsPerIdWithDefault(node).add(currPath.snapshot, 1)
  }

  /**
   * Reset the current path
   */
  def resetCurrentPath() {
    currPath.clear()
  }

  /**
   * @return a list of tuple(path, count) where count is the number of times path
   *         ending at {@code node} is observed
   */
  def topPathsTill(node: Int, num: Int) = {
    val pathCountMap = pathCountsPerIdWithDefault(node)
    val pathCount = pathCountMap.size

    // Save direct path values into an array
    val pathArray = new Array[DirectedPath](pathCount)
    pathCountMap.keySet.toArray(pathArray)

    val sorted = pathArray.toSeq.sortBy { x => -1 * pathCountMap.getInt(x) }
    sorted.take(num).map { path => (path, pathCountMap.getInt(path)) }.toArray
  }

  /**
   * @param num the number of top paths to return for a node
   * @return an array of tuples, each containing a node and array of top paths ending at node, with scores
   */
  def topPathsPerNodeId(num: Int): Array[(Int, Array[(DirectedPath, Int)])] = {
    val idsWithTopPaths = new Array[(Int, Array[(DirectedPath, Int)])](pathCountsPerId.size)

    val nodeIterator = pathCountsPerId.keySet.iterator
    var counter = 0
    while (nodeIterator.hasNext) {
      val node = nodeIterator.nextInt
      idsWithTopPaths(counter) = (node, topPathsTill(node, num))
      counter += 1
    }

    idsWithTopPaths
  }

  /**
   * @return the total number of unique paths that end at {@code node}
   */
  def numUniquePathsTill(node: Int) = pathCountsPerIdWithDefault(node).size

  /**
   * @return the total number of unique paths in this collection
   */
  def totalNumPaths = {
    var sum = 0
    val iterator = pathCountsPerId.keySet.iterator
    while (iterator.hasNext) {
      val node = iterator.nextInt
      sum += numUniquePathsTill(node)
    }
    sum
  }

  private def pathCountsPerIdWithDefault(node: Int) = {
    if (!pathCountsPerId.containsKey(node)) {
      val map = new Object2IntOpenHashMap[DirectedPath]
      pathCountsPerId.put(node, map)
    }
    pathCountsPerId.get(node)
  }

}

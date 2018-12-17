package student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import game.GetOutState;
import game.FindState;
import game.SewerDiver;
import game.Node;
import game.NodeStatus;

public class DiverMax extends SewerDiver {


    /** Get to the ring in as few steps as possible. Once you get there, 
     * you must return from this function in order to pick
     * it up. If you continue to move after finding the ring rather 
     * than returning, it will not count.
     * If you return from this function while not standing on top of the ring, 
     * it will count as a failure.
     * 
     * There is no limit to how many steps you can take, but you will receive
     * a score bonus multiplier for finding the ring in fewer steps.
     * 
     * At every step, you know only your current tile's ID and the ID of all 
     * open neighbor tiles, as well as the distance to the ring at each of these tiles
     * (ignoring walls and obstacles). 
     * 
     * In order to get information about the current state, use functions
     * currentLocation(), neighbors(), and distanceToRing() in FindState.
     * You know you are standing on the ring when distanceToRing() is 0.
     * 
     * Use function moveTo(long id) in FindState to move to a neighboring 
     * tile by its ID. Doing this will change state to reflect your new position.
     * 
     * A suggested first implementation that will always find the ring, but likely won't
     * receive a large bonus multiplier, is a depth-first walk. Some
     * modification is necessary to make the search better, in general.*/
	@Override public void findRing(FindState state) {
		//TODO : Find the ring and return.
		// DO NOT WRITE ALL THE CODE HERE. DO NOT MAKE THIS METHOD RECURSIVE.
		// Instead, write your method elsewhere, with a good specification,
		// and call it from this one.
		
		ArrayList<Long> visited = new ArrayList<>();
		DFS(state, visited, 0);
	}
	
	/**
	 * Depth first search through the sewer system that recursively runs until
	 * the ring is found.
	 * @param s gives the current FindState
	 * @param visited stores all spots that have been previously visited
	 * @param last gives the previously visited Node
	 */
	public void DFS(FindState s, ArrayList<Long> visited, long last) {
		if(s.distanceToRing()==0) return;
		Heap<NodeStatus> priority = new Heap<NodeStatus>();
		long curr = s.currentLocation();
		visited.add(curr);
		long newLast = curr;
		for (NodeStatus n: s.neighbors()) {
			if(!visited.contains(n.getId())) {
				priority.add(n, n.getDistanceToTarget());
			}
		}
		while(priority.size > 0) {
			NodeStatus nextTile = priority.poll();
			s.moveTo(nextTile.getId());
			
			visited.add(s.currentLocation());
			DFS(s, visited, newLast);
			if(s.distanceToRing()==0) return;
		}
		s.moveTo(last);
	}


    /** Get out of the sewer system before the steps are all used, trying to collect
     * as many coins as possible along the way. Your solution must ALWAYS get out
     * before the steps are all used, and this should be prioritized above
     * collecting coins.
     * 
     * You now have access to the entire underlying graph, which can be accessed
     * through GetOutState. currentNode() and getExit() will return Node objects
     * of interest, and getNodes() will return a collection of all nodes on the graph. 
     * 
     * You have to get out of the sewer system in the number of steps given by
     * getStepsRemaining(); for each move along an edge, this number is decremented
     * by the weight of the edge taken.
     * 
     * Use moveTo(n) to move to a node n that is adjacent to the current node.
     * When n is moved-to, coins on node n are automatically picked up.
     * 
     * You must return from this function while standing at the exit. Failing to
     * do so before steps run out or returning from the wrong node will be
     * considered a failed run.
     * 
     * Initially, there are enough steps to get from the starting point to the
     * exit using the shortest path, although this will not collect many coins.
     * For this reason, a good starting solution is to use the shortest path to
     * the exit. */
    @Override public void getOut(GetOutState state) {
        //TODO: Get out of the sewer system before the steps are used up.
        // DO NOT WRITE ALL THE CODE HERE. Instead, write your method elsewhere,
        //with a good specification, and call it from this one.
    	
    	Collection<Node> allNodes = state.allNodes();
 		Node exit = state.getExit();
 		Node current = state.currentNode();
    	int big = 0;
 		for (Node n : allNodes) {
	   		int c = n.getTile().coins();
	   		if (c>big) big=c;
	    }
    	
    	HashSet<Node> visited = new HashSet<Node>();
    	HashSet<Node> inHeap = inHeap(state);
    	Heap<Node> mostCoins = createHeap(state, inHeap);
    	
 		int startSize = mostCoins.size();
 		
 		int cnt=0;
 		while (mostCoins.size()>0 && cnt<startSize) {
 			Node next = mostCoins.poll();
 			inHeap.remove(next);
 			visited.add(current);
 		
 			if(!visited.contains(next)) {
 				List<Node> currPath1 =Paths.minPath(current,next);
 				int currPath1Len = Paths.sumPath(currPath1);
 				List<Node> currPath2 = Paths.minPath(next,exit);
 				int currPath2Len = Paths.sumPath(currPath2);
 				int stepsLeft = state.stepsLeft();
 				if(stepsLeft >= currPath1Len + currPath2Len) {
 					for (int i=1; i<currPath1.size(); i++) {
 						current = state.currentNode();
 						Node n = currPath1.get(i);
 						if (current!=n) {
 	 						visited.add(current);
 							state.moveTo(n);
 						}
 		 	    	}
 				}
 				current = state.currentNode();
 				//limited amount of updates on the heap in order to control runtime
 				if (cnt<10) mostCoins = createHeap(state, inHeap);
 				if (cnt==10)updateHeap(state,mostCoins,big,inHeap);
 			}
 			cnt++;
 		}
 		if(state.currentNode() != exit) {
 			List<Node> currPath = Paths.minPath(current,exit);
 			for (int i=1; i<currPath.size(); i++) {
 	       		if (state.currentNode()!=currPath.get(i))state.moveTo(currPath.get(i));
 	    	}
 		}
 	
 		return;
    }
    
    /**
     * Creates a heap that determines the priority for which nodes should be
     * targeted for the diver to travel to on the way out. For all nodes that
     * should be in the heap, the method calculates how many coins would be
     * picked up on the way for each node. Nodes that allow for more coins
     * to be picked up will be given higher priority in the heap.
     * @param s gives the current GetOutState
     * @param inHeap gives us a HashSet containing all the nodes that should
     * be included in the heap.
     * @return a heap with highest priority to nodes that should be targeted
     * first on the way out.
     */
    public Heap<Node> createHeap(GetOutState s, HashSet<Node> inHeap) {
    	Heap<Node> order = new Heap<>();
    	Collection<Node> all = s.allNodes();
    	Node curr = s.currentNode();
    	for (Node n: all) {
    		if (inHeap.contains(n)) {
	    		int c = n.getTile().coins();
				List<Node> minPath = Paths.minPath(curr,n);
				int val = 0;
				for (Node w : minPath) {
					val += w.getTile().coins();
				}
	    		if (c>0) order.add(n, Integer.MAX_VALUE-val);
    		}
    	}
    	return order;
    }
    
    /**
     * Updates the heap by changing the priorities for each node based on how
     * many coins the node contains and how far the node is from the current 
     * location of the diver.
     * @param s gives the current GetOutState
     * @param mostCoins gives us the heap which should be updated
     * @param big gives us the highest number of coins on a single node in the 
     * sewer system
     * @param inHeap gives us a HashSet containing all the nodes that should
     * be included in the heap.
     */
    public void updateHeap(GetOutState s, Heap<Node> mostCoins, double big, 
    		HashSet<Node> inHeap) {
    	Collection<Node> all = s.allNodes();
    	Node curr = s.currentNode();
    	for (Node n: all) {
    		if (inHeap.contains(n)) {
	    		int c = n.getTile().coins();
				List<Node> minPath = Paths.minPath(curr,n);
				int val = Paths.sumPath(minPath);
	    		if (c>0) mostCoins.changePriority(n, (big-c)*val);
    		}
    	}
    }
    
    /**
     * Used to determine which nodes should be added to the heap in the
     * createHeap and updateHeap methods.
     * @param s gives the current GetOutState
     * @return a HashSet containing all the nodes that should be in the Heap.
     */
    public HashSet<Node> inHeap(GetOutState s) {
    	HashSet<Node> order = new HashSet<>();
    	Collection<Node> all = s.allNodes();
    	for (Node n: all) {
    		int c = n.getTile().coins();
    		if (c>0) order.add(n);
    	}
    	return order;
    }
    
}

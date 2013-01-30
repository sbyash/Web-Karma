/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.modeling.research;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import edu.isi.karma.modeling.alignment.GraphUtil;
import edu.isi.karma.rep.alignment.ColumnNode;
import edu.isi.karma.rep.alignment.Link;
import edu.isi.karma.rep.alignment.LiteralNode;
import edu.isi.karma.rep.alignment.Node;

public class ServiceModel {

	private String serviceNameWithPrefix;
	private String serviceName;
	private String serviceDescription;
	
	private List<DirectedWeightedMultigraph<Node, Link>> models;
	private HashMap<String, List<DijkstraShortestPath<Node, Link>>> shortestPathsBetweenTwoAttributes; 
	private List<MatchedSubGraphs> matchedSubGraphs;

	public ServiceModel() {
		this.models = new ArrayList<DirectedWeightedMultigraph<Node,Link>>();
		matchedSubGraphs = new ArrayList<MatchedSubGraphs>();
		shortestPathsBetweenTwoAttributes = new HashMap<String, List<DijkstraShortestPath<Node,Link>>>();
	}

	
	public String getServiceName() {
		return serviceName;
	}


	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}


	public String getServiceNameWithPrefix() {
		return serviceNameWithPrefix;
	}


	public void setServiceNameWithPrefix(String serviceNameWithPrefix) {
		this.serviceNameWithPrefix = serviceNameWithPrefix;
	}


	public String getServiceDescription() {
		return serviceDescription;
	}


	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}


	public List<DirectedWeightedMultigraph<Node,Link>> getModels() {
		return models;
	}
	
	public void addModel(DirectedWeightedMultigraph<Node, Link> graph) {
		this.models.add(graph);
	}
	
	public void print() {
		System.out.println(this.getServiceName());
		System.out.println();
		for (DirectedWeightedMultigraph<Node, Link> g : this.getModels())
				GraphUtil.printGraphSimple(g);
		System.out.println();
		
		List<String> sortedKeys = Arrays.asList(shortestPathsBetweenTwoAttributes.keySet().toArray(new String[0]));
		Collections.sort(sortedKeys);
		
		String lastNodeId = "", nextId = "", currentId = "";
		for (String index : sortedKeys) {

			System.out.println(index + ": ");
			for (DijkstraShortestPath<Node,Link> path : shortestPathsBetweenTwoAttributes.get(index)) {
				
				List<Link> pathEdges = path.getPathEdgeList();
				lastNodeId = ""; nextId = ""; currentId = "";
				if (pathEdges == null)
					continue;
				for (int i = 0; i < pathEdges.size(); i++) {
					
					Link e = pathEdges.get(i);
					
					if (i == 0) {
						currentId = e.getSource().getId();
						nextId = e.getTarget().getId();
						if (pathEdges.size() > 1) {
							Link nextEdge = pathEdges.get(1);
							if (e.getSource().getId().equalsIgnoreCase(nextEdge.getSource().getId()) ||
									e.getSource().getId().equalsIgnoreCase(nextEdge.getTarget().getId())) {
								currentId = e.getTarget().getId();
								nextId = e.getSource().getId();
							}
						}
						System.out.print("\t");
						System.out.print("(");
						System.out.print(currentId);
						System.out.print(")");
					} else if (e.getSource().getId().equalsIgnoreCase(lastNodeId)) {
						nextId = e.getTarget().getId();
					} else if (e.getTarget().getId().equalsIgnoreCase(lastNodeId)) {
						nextId = e.getSource().getId();
					}
					lastNodeId = nextId;
					System.out.print("---");
					System.out.print(e.getId());
					System.out.print("---");
					System.out.print("(");
					System.out.print(nextId);
					System.out.print(")");
				}

			}
		}
		System.out.println();
		System.out.println();
	}

	public void computeMatchedSubGraphs() {
		if (this.models.size() == 2) {
			this.matchedSubGraphs = 
					Algorithm.computeMatchedSubGraphs(this.models.get(0), this.models.get(1));
		}
	}
	
	public void computeShortestPaths() {
		
		int modelNo = 1;
		DijkstraShortestPath<Node, Link> path;
		
		for (DirectedWeightedMultigraph<Node, Link> graph : this.models) {
			
			List<Node> attributes = Util.getAttributes(graph);
			for (int i = 0; i < attributes.size(); i++) {
				for (int j = i+1; j < attributes.size(); j++) {
					
					Node source = attributes.get(i);
					Node target = attributes.get(j);
					String index = source.getId().replaceAll(ModelReader.attPrefix, "") + 
									"-->" + 
									target.getId().replaceAll(ModelReader.attPrefix, "") + 
									" (m" + modelNo + ")";
					
					// TODO: How to get all the shortest paths?
					UndirectedGraph<Node, Link> undirectedGraph = 
							new AsUndirectedGraph<Node, Link>(graph);	
					
					path = new DijkstraShortestPath<Node, Link>(undirectedGraph, source, target);
					
					List<DijkstraShortestPath<Node,Link>> paths = 
							this.shortestPathsBetweenTwoAttributes.get(index);
					
					if (paths == null) {
						paths = new ArrayList<DijkstraShortestPath<Node,Link>>();
						this.shortestPathsBetweenTwoAttributes.put(index, paths);
					}
					
					paths.add(path);
				}
			}
			
			modelNo ++;
		}
	}
	
	public void exportModelsToGraphviz(String exportDirectory) throws FileNotFoundException {
		
		OutputStream out = new FileOutputStream(exportDirectory + this.getServiceNameWithPrefix() + "_models.dot");
		org.kohsuke.graphviz.Graph graphViz = new org.kohsuke.graphviz.Graph();
		
		graphViz.attr("fontcolor", "blue");
		graphViz.attr("remincross", "true");
		graphViz.attr("label", this.getServiceDescription());
//		graphViz.attr("page", "8.5,11");

		int modelNo = 1;
		for (DirectedWeightedMultigraph<Node, Link> model : this.models) {
			org.kohsuke.graphviz.Graph gViz = exportJGraphToGraphviz(model);
			gViz.attr("label", "model_" + modelNo);
			gViz.id("cluster_" + modelNo);
			graphViz.subGraph(gViz);
			modelNo ++;
		}
		graphViz.writeTo(out);


	}
	
	public void exportMatchedSubGraphToGraphviz(String exportDirectory) throws FileNotFoundException {
		
		if (this.matchedSubGraphs == null) return;
		
		OutputStream out = new FileOutputStream(exportDirectory + this.getServiceNameWithPrefix() + "_subgraphs.dot");
		org.kohsuke.graphviz.Graph graphViz = new org.kohsuke.graphviz.Graph();
		graphViz.attr("fontcolor", "blue");
		graphViz.attr("remincross", "true");
		graphViz.attr("label", this.getServiceDescription());
//		graphViz.attr("page", "8.5,11");
		
		List<String> sortedKeys = Arrays.asList(shortestPathsBetweenTwoAttributes.keySet().toArray(new String[0]));
		Collections.sort(sortedKeys);

		org.kohsuke.graphviz.Graph cluster = null;
		org.kohsuke.graphviz.Graph gViz = null;
		int counter = 0;
		for (MatchedSubGraphs m : this.matchedSubGraphs) {

			cluster = new org.kohsuke.graphviz.Graph();
			cluster.id("cluster_" + counter);
			cluster.attr("label", "");
			graphViz.subGraph(cluster);

			gViz = exportJGraphToGraphviz(m.getSubGraph1());
			cluster.subGraph(gViz);
			gViz = exportJGraphToGraphviz(m.getSubGraph2());
			cluster.subGraph(gViz);
			counter ++;
		}
		graphViz.writeTo(out);

	}

	
	public void exportShortestPathsToGraphviz(String exportDirectory) throws FileNotFoundException {
		
		OutputStream out = new FileOutputStream(exportDirectory + this.getServiceNameWithPrefix() + "_paths.dot");
		org.kohsuke.graphviz.Graph graphViz = new org.kohsuke.graphviz.Graph();
		graphViz.attr("fontcolor", "blue");
		graphViz.attr("remincross", "true");
		graphViz.attr("label", this.getServiceDescription());
//		graphViz.attr("page", "8.5,11");
		
		List<String> sortedKeys = Arrays.asList(shortestPathsBetweenTwoAttributes.keySet().toArray(new String[0]));
		Collections.sort(sortedKeys);

		org.kohsuke.graphviz.Graph cluster = null;
		int counter = 0;
		for (String index : sortedKeys) {

			if (counter % 2 == 0) { 
				cluster = new org.kohsuke.graphviz.Graph();
				cluster.id("cluster_" + counter);
				cluster.attr("label", index.substring(0, index.indexOf("(")).trim());
				graphViz.subGraph(cluster);
			}

			org.kohsuke.graphviz.Graph gViz = exportJGrapPathToGraphviz(this.shortestPathsBetweenTwoAttributes.get(index).get(0));
			gViz.attr("label", index.substring(index.indexOf("(") + 1, index.indexOf(")")) );
			gViz.id("model_" + (counter % 2 + 1) );
			cluster.subGraph(gViz);
			counter ++;
			
		}
		graphViz.writeTo(out);

	}
	
	private org.kohsuke.graphviz.Graph exportJGrapPathToGraphviz(DijkstraShortestPath<Node, Link> path) {

		org.kohsuke.graphviz.Graph gViz = new org.kohsuke.graphviz.Graph();
			
		org.kohsuke.graphviz.Style internalNodeStyle = new org.kohsuke.graphviz.Style();
//		internalNodeStyle.attr("shape", "circle");
		internalNodeStyle.attr("style", "filled");
		internalNodeStyle.attr("color", "white");
		internalNodeStyle.attr("fontsize", "10");
		internalNodeStyle.attr("fillcolor", "lightgray");
		
//		org.kohsuke.graphviz.Style inputNodeStyle = new org.kohsuke.graphviz.Style();
//		inputNodeStyle.attr("shape", "plaintext");
//		inputNodeStyle.attr("style", "filled");
//		inputNodeStyle.attr("fillcolor", "#3CB371");
//
//		org.kohsuke.graphviz.Style outputNodeStyle = new org.kohsuke.graphviz.Style();
//		outputNodeStyle.attr("shape", "plaintext");
//		outputNodeStyle.attr("style", "filled");
//		outputNodeStyle.attr("fillcolor", "gold");

		org.kohsuke.graphviz.Style parameterNodeStyle = new org.kohsuke.graphviz.Style();
		parameterNodeStyle.attr("shape", "plaintext");
		parameterNodeStyle.attr("style", "filled");
		parameterNodeStyle.attr("fillcolor", "gold");

		org.kohsuke.graphviz.Style literalNodeStyle = new org.kohsuke.graphviz.Style();
		literalNodeStyle.attr("shape", "plaintext");
		literalNodeStyle.attr("style", "filled");
		literalNodeStyle.attr("fillcolor", "#CC7799");

		org.kohsuke.graphviz.Style edgeStyle = new org.kohsuke.graphviz.Style();
		edgeStyle.attr("color", "brown");
		edgeStyle.attr("fontsize", "10");
		edgeStyle.attr("fontcolor", "black");
		
		HashMap<Node, org.kohsuke.graphviz.Node> nodeIndex = new HashMap<Node, org.kohsuke.graphviz.Node>();
		
//		Node lastNode = null;
		for (int i = 0; i < path.getPathEdgeList().size(); i++) {
			
			Link e = path.getPathEdgeList().get(i);
			
			Node source = e.getSource();
			Node target = e.getTarget();
			
			org.kohsuke.graphviz.Node n = nodeIndex.get(source);
			String id = source.getId();
			if (n == null) {
				n = new org.kohsuke.graphviz.Node();
				n.attr("label", id);
				nodeIndex.put(source, n);
			
//				if (id.indexOf("att") != -1 && id.indexOf("i") != -1) // input
//					gViz.nodeWith(inputNodeStyle);
//				else if (id.indexOf("att") != -1 && id.indexOf("o") != -1)  // output
//					gViz.nodeWith(outputNodeStyle);
				if (source instanceof ColumnNode)  // attribute
					gViz.nodeWith(parameterNodeStyle);
				else if (source instanceof LiteralNode)  // literal
					gViz.nodeWith(literalNodeStyle);
				else  // internal node
					gViz.nodeWith(internalNodeStyle);
					
				gViz.node(n);
			}

			n = nodeIndex.get(target);
			id = target.getId();
			if (n == null) {
				n = new org.kohsuke.graphviz.Node();
				n.attr("label", id);
				nodeIndex.put(target, n);
			
//				if (id.indexOf("att") != -1 && id.indexOf("i") != -1) // input
//					gViz.nodeWith(inputNodeStyle);
//				else if (id.indexOf("att") != -1 && id.indexOf("o") != -1)  // output
//					gViz.nodeWith(outputNodeStyle);
				if (target instanceof ColumnNode)  // attribute
					gViz.nodeWith(parameterNodeStyle);
				else if (target instanceof LiteralNode)  // literal
					gViz.nodeWith(literalNodeStyle);
				else  // internal node
					gViz.nodeWith(internalNodeStyle);
					
				gViz.node(n);
			}
			
			/*
			org.kohsuke.graphviz.Edge edge = null;
			if (i == 0) {
				edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(source), nodeIndex.get(target));
				lastNode = target;
				if (path.getPathEdgeList().size() > 1) {
					Link nextEdge = path.getPathEdgeList().get(1);
					if (source.equals(nextEdge.getSource()) ||
							source.equals(nextEdge.getTarget())) {
						edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(target), nodeIndex.get(source));
						lastNode = source;					}
				}
			} else if (target.equals(lastNode)) {
				edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(target), nodeIndex.get(source));
				lastNode = source;
			} else if (source.equals(lastNode)) {
				edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(source), nodeIndex.get(target));
				lastNode = target;
			} 
			*/
			
			org.kohsuke.graphviz.Edge edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(source), nodeIndex.get(target));
			edge.attr("label", e.getId());
			gViz.edgeWith(edgeStyle);
			gViz.edge(edge);
		}


		return gViz;
	}
	
	private org.kohsuke.graphviz.Graph exportJGraphToGraphviz(DirectedWeightedMultigraph<Node, Link> model) {

		org.kohsuke.graphviz.Graph gViz = new org.kohsuke.graphviz.Graph();
			
		org.kohsuke.graphviz.Style internalNodeStyle = new org.kohsuke.graphviz.Style();
//		internalNodeStyle.attr("shape", "circle");
		internalNodeStyle.attr("style", "filled");
		internalNodeStyle.attr("color", "white");
		internalNodeStyle.attr("fontsize", "10");
		internalNodeStyle.attr("fillcolor", "lightgray");
		
//		org.kohsuke.graphviz.Style inputNodeStyle = new org.kohsuke.graphviz.Style();
//		inputNodeStyle.attr("shape", "plaintext");
//		inputNodeStyle.attr("style", "filled");
//		inputNodeStyle.attr("fillcolor", "#3CB371");
//
//		org.kohsuke.graphviz.Style outputNodeStyle = new org.kohsuke.graphviz.Style();
//		outputNodeStyle.attr("shape", "plaintext");
//		outputNodeStyle.attr("style", "filled");
//		outputNodeStyle.attr("fillcolor", "gold");

		org.kohsuke.graphviz.Style parameterNodeStyle = new org.kohsuke.graphviz.Style();
		parameterNodeStyle.attr("shape", "plaintext");
		parameterNodeStyle.attr("style", "filled");
		parameterNodeStyle.attr("fillcolor", "gold");

		org.kohsuke.graphviz.Style literalNodeStyle = new org.kohsuke.graphviz.Style();
		literalNodeStyle.attr("shape", "plaintext");
		literalNodeStyle.attr("style", "filled");
		literalNodeStyle.attr("fillcolor", "#CC7799");

		org.kohsuke.graphviz.Style edgeStyle = new org.kohsuke.graphviz.Style();
		edgeStyle.attr("color", "brown");
		edgeStyle.attr("fontsize", "10");
		edgeStyle.attr("fontcolor", "black");
		
		HashMap<Node, org.kohsuke.graphviz.Node> nodeIndex = new HashMap<Node, org.kohsuke.graphviz.Node>();
		
		for (Link e : model.edgeSet()) {
			
			Node source = e.getSource();
			Node target = e.getTarget();
			
			org.kohsuke.graphviz.Node n = nodeIndex.get(source);
			String id = source.getId();
			if (n == null) {
				n = new org.kohsuke.graphviz.Node();
				n.attr("label", id);
				nodeIndex.put(source, n);
			
//				if (id.indexOf("att") != -1 && id.indexOf("i") != -1) // input
//					gViz.nodeWith(inputNodeStyle);
//				else if (id.indexOf("att") != -1 && id.indexOf("o") != -1)  // output
//					gViz.nodeWith(outputNodeStyle);
				if (source instanceof ColumnNode)  // attribute
					gViz.nodeWith(parameterNodeStyle);
				else if (source instanceof LiteralNode)  // literal
					gViz.nodeWith(literalNodeStyle);
				else  // internal node
					gViz.nodeWith(internalNodeStyle);
					
				gViz.node(n);
			}

			n = nodeIndex.get(target);
			id = target.getId();
			if (n == null) {
				n = new org.kohsuke.graphviz.Node();
				n.attr("label", id);
				nodeIndex.put(target, n);
			
//				if (id.indexOf("att") != -1 && id.indexOf("i") != -1) // input
//					gViz.nodeWith(inputNodeStyle);
//				else if (id.indexOf("att") != -1 && id.indexOf("o") != -1)  // output
//					gViz.nodeWith(outputNodeStyle);
				if (target instanceof ColumnNode)  // attribute
					gViz.nodeWith(parameterNodeStyle);
				else if (target instanceof LiteralNode)  // literal
					gViz.nodeWith(literalNodeStyle);
				else  // internal node
					gViz.nodeWith(internalNodeStyle);
					
				gViz.node(n);
			}
			
			org.kohsuke.graphviz.Edge edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(source), nodeIndex.get(target));
			edge.attr("label", e.getId());
			gViz.edgeWith(edgeStyle);
			gViz.edge(edge);
		}


		return gViz;
	}
	
	
}
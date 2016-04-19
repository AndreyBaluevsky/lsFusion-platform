package lsfusion.utils;

import java.util.*;

/**
 *  Реализует алгоритм построения бинарного дерева по набору вершин.
 *  <p>
 *  Входные данные: исходный граф G(V, E) - набор вершин и заданных ребер между ними. 
 *  Также необходима оценочная функция - реализация интерфейса CalculateCost, результат выполнения которой
 *  зависит от двух вершин изменяемого графа G'(N, E') и набора ребер исходного графа G.
 *  <p>
 *  Алгоритм: 
 *  По исходному графу G, строим новый граф G'(N, E'), вершинами которого являются объекты класса Node, которые
 *  представляют собой подможножество вершин исходного графа G. Изначально каждой вершине исходного графа соответствует
 *  вершина графа G'. Граф G' всегда полный. 
 *  Затем находим стоимость каждого ребра в полном графе G' с помощью вызова оценочной функции, если в исходном графе не было
 *  ребер между вершинами, то в функцию передается пустой набор ребер.
 *  Затем на каждом шаге находим минимальное ребро, и объединяем вершины этого ребра в одну вершину. При этом две вершины
 *  графа G' удаляются и добавляется новая, для которой пересчитываются все ребра с остальными вершинами графа. Новой 
 *  вершине устанавливается стоимость, равная стоимости минимального ребра. 
 */

public class GreedyTreeBuilding<V, C extends Comparable<C>> {
    public interface Edge<V> {
        V getFrom();
        V getTo();
    }
    
    public interface CalculateCost<V, C extends Comparable<C>> {
        C calculate(Node<V, C> a, Node<V, C> b, Iterable<Edge<V>> edges); 
    }
    
    /** Вершина изменяющегося графа, является объединением некоторого подмножества вершин исходного графа */ 
    static public class Node<V, C> {
        private boolean isInner;
        private final V initialVertex;
        private final C cost;
        
        public Node<V, C> left = null;
        public Node<V, C> right = null; 
        
        public Node(V vertex, C cost, Node<V, C> left, Node<V, C> right) {
            this.initialVertex = vertex;
            this.cost = cost;
            this.left = left;
            this.right = right;
            isInner = (left != null || right != null);
        } 
        
        public Node(V vertex, C cost) {
            this(vertex, cost, null, null);
        }
        
        public boolean isInner() {
            return isInner;
        }
        
        public V getVertex() {
            return (isInner ? null : initialVertex);
        }
        
        public C getCost() {
            return cost;
        }
    }

    /** Ребро исходного графа (по умолчанию) */
    static class SimpleEdge<V> implements Edge<V> {
        private final V from, to;
        
        public SimpleEdge(V from, V to) {
            this.from = from;
            this.to = to;
        }
        
        @Override
        public V getFrom() {
            return from;
        }

        @Override
        public V getTo() {
            return to;
        }
    }
    
    /** Ребро изменяющегося графа, являющееся объединением ребер исходного графа */ 
    static class ComplexEdge<V, C extends Comparable<C>> implements Edge<Node<V, C>>, Comparable<ComplexEdge<V, C>> {
        private final C cost;
        private final Node<V, C> from;
        private final Node<V, C> to;
        private EdgeLinkedList<Edge<V>> simpleEdges;
        
        public ComplexEdge(Node<V, C> from, Node<V, C> to, EdgeLinkedList<Edge<V>> simpleEdges, C cost) {
            this.from = from;
            this.to = to;
            this.cost = cost;
            this.simpleEdges = simpleEdges;
        }
        
        @Override
        public int compareTo(ComplexEdge<V, C> o) {
            return cost.compareTo(o.getCost());
        }

        @Override
        public Node<V, C> getFrom() {
            return from;
        }

        @Override
        public Node<V, C> getTo() {
            return to;
        }

        public C getCost() {
            return cost;
        }
        
        public EdgeLinkedList<Edge<V>> mergeSimpleEdges(ComplexEdge<V, C> otherEdge) {
            simpleEdges.addList(otherEdge.simpleEdges);
            return simpleEdges;
        }
    }
    
    private static class EdgeLinkedList<E extends Edge> implements Iterable<E> {
        @Override
        public Iterator<E> iterator() {
            return new EdgeLinkedListIterator();
        }

        private class EdgeLinkedListIterator implements Iterator<E> {
            private ListNode<E> node;
            
            public EdgeLinkedListIterator() {
                node = head;
            }

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ListNode<E> tmp = node;
                node = node.next;
                return tmp.value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        } 
        
        public static class ListNode<E extends Edge> {
            public ListNode(E value) { this.value = value; }
            public ListNode<E> next;
            public E value; 
        }
        
        private ListNode<E> head;
        private ListNode<E> tail;
        
        public ListNode<E> getHead() {
            return head;
        }
        
        public ListNode<E> getTail() {
            return tail;
        } 
        
        public void addEdge(E edge) {
            ListNode<E> newNode = new ListNode<>(edge);    
            if (head == null) {
                head = newNode;
            } 
            if (tail != null) {
                tail.next = newNode;
            }
            tail = newNode;
        }
        
        public void addList(EdgeLinkedList<E> secondList) {
            if (secondList.getHead() == null) {
                return;
            }
            if (head == null) {
                head = secondList.getHead();
                tail = secondList.getTail();
            } else {
                tail.next = secondList.getHead();
                tail = secondList.getTail();
            }                
        }
    }
    
    private List<Collection<Edge<V>>> adjList = new ArrayList<>();
    private List<C> costs = new ArrayList<>(); 
    private List<V> vertices = new ArrayList<>();
    private Map<V, Integer> vertexIndex = new HashMap<>(); 
    
    public void addEdge(Edge<V> e) {
        assert vertexIndex.containsKey(e.getFrom());
        assert vertexIndex.containsKey(e.getTo());
        
        int fromIndex = vertexIndex.get(e.getFrom());
        adjList.get(fromIndex).add(e);
    }
    
    public void addVertex(V vertex, C cost) {
        assert !vertexIndex.containsKey(vertex);
        vertices.add(vertex);
        vertexIndex.put(vertex, vertexIndex.size());
        adjList.add(new ArrayList<Edge<V>>());
        costs.add(cost);
    }

    private PriorityQueue<ComplexEdge<V, C>> queue = new PriorityQueue<>();
    private final Map<Node<V, C>, Integer> nodeIndex = new HashMap<>();
    private final List<Node<V, C>> nodes = new ArrayList<>();
    private List<List<ComplexEdge<V, C>>> adjMatrix = new ArrayList<>(); 
    
    private Node<V, C> addNode(V vertex, C cost, Node<V, C> leftChild, Node<V, C> rightChild) {
        Node<V, C> node = new Node<>(vertex, cost, leftChild, rightChild);
        nodes.add(node);
        nodeIndex.put(node, nodes.size()-1);
        return node;
    }
    
    private void initNodes() {
        nodeIndex.clear();
        nodes.clear();
        
        for (int i = 0; i < vertices.size(); ++i) {
            addNode(vertices.get(i), costs.get(i), null, null);
        }
    }
    
    private void initEdges(CalculateCost<V, C>  functor) {
        queue.clear();
        adjMatrix.clear();

        final int vertexCnt = vertices.size();
        
        for (int i = 0; i < vertexCnt; ++i) {
            adjMatrix.add(new ArrayList<>(Collections.<ComplexEdge<V, C>>nCopies(vertexCnt, null)));
        }
        
        for (int i = 0; i < vertexCnt; ++i) {
            for (Edge<V> edge : adjList.get(i)) {
                int fromIndex = vertexIndex.get(edge.getFrom());
                int toIndex = vertexIndex.get(edge.getTo());
                EdgeLinkedList<Edge<V>> simpleEdges = new EdgeLinkedList<>();
                simpleEdges.addEdge(edge);
                C edgeCost = functor.calculate(nodes.get(fromIndex), nodes.get(toIndex), simpleEdges);
                ComplexEdge<V, C> newEdge = new ComplexEdge<>(nodes.get(fromIndex), nodes.get(toIndex), simpleEdges, edgeCost);
                
                adjMatrix.get(fromIndex).set(toIndex, newEdge);
                adjMatrix.get(toIndex).set(fromIndex, newEdge);
                queue.add(newEdge);
            }
        }

        for (int i = 0; i < vertexCnt; ++i) {
            for (int j = i+1; j < vertexCnt; ++j) {
                if (adjMatrix.get(i).get(j) == null) {
                    C edgeCost = functor.calculate(nodes.get(i), nodes.get(j), new LinkedList<Edge<V>>());
                    ComplexEdge<V, C> newEdge = new ComplexEdge<>(nodes.get(i), nodes.get(j), new EdgeLinkedList<Edge<V>>(), edgeCost);
                    adjMatrix.get(i).set(j, newEdge);
                    adjMatrix.get(j).set(i, newEdge);
                    queue.add(newEdge);
                }
            }
        }
    }

    private ComplexEdge<V, C> getMinimumEdge(Set<Node<V, C>> deleted) {
        while (!queue.isEmpty()) {
            ComplexEdge<V, C> nextEdge = queue.poll();
            if (!deleted.contains(nextEdge.getFrom()) && !deleted.contains(nextEdge.getTo())) {
                return nextEdge;
            }
        } 
        return null;
    }
    
    private void joinNodes(ComplexEdge<V, C> edge, Set<Node<V, C>> deleted, CalculateCost<V, C> functor) {
        int fromIndex = nodeIndex.get(edge.getFrom());
        int toIndex = nodeIndex.get(edge.getTo());
        
        Node<V, C> newNode = addNode(null, edge.getCost(), edge.getFrom(), edge.getTo());
        int newIndex = nodeIndex.get(newNode);
        
        int nodeCnt = adjMatrix.size();
        adjMatrix.add(new ArrayList<>(Collections.<ComplexEdge<V, C>>nCopies(nodeCnt + 1, null)));
        for (int i = 0; i < nodeCnt; ++i) {
            if (i != fromIndex && i != toIndex && !deleted.contains(nodes.get(i))) {
                EdgeLinkedList<Edge<V>> edges = adjMatrix.get(fromIndex).get(i).mergeSimpleEdges(adjMatrix.get(toIndex).get(i));
                C cost = functor.calculate(newNode, nodes.get(i), edges);
                ComplexEdge<V, C> newEdge = new ComplexEdge<>(newNode, nodes.get(i), edges, cost);
                adjMatrix.get(newIndex).set(i, newEdge);
                adjMatrix.get(i).add(newEdge);
                queue.add(newEdge);
            }
        }
        
        deleted.add(edge.getFrom());
        deleted.add(edge.getTo());
    } 
    
    public Node<V, C> compute(CalculateCost<V, C> functor) {
        initNodes();        
        initEdges(functor);
        
        Set<Node<V, C>> deletedNodes = new HashSet<>();
        for (int i = 0; i + 1 < vertices.size(); ++i) {
            ComplexEdge<V, C> nextEdge = getMinimumEdge(deletedNodes);
            assert nextEdge != null;
           
            joinNodes(nextEdge, deletedNodes, functor);
        }
        return nodes.get(nodes.size() - 1); 
    }

    private int indexFromMask(int mask) {
        return (int)Math.round(Math.log(mask) / Math.log(2.0));        
    }
    
    private Node<V, C> DP(int mask, ArrayList<Node<V, C>> memo, CalculateCost<V, C> functor) {
        if (memo.get(mask) != null) {
            return memo.get(mask);
        }
        if ((mask & (mask-1)) == 0) { // если подмножество состоит из одного элемента
            int nodeIndex = indexFromMask(mask);
            memo.set(mask, nodes.get(nodeIndex));
            return nodes.get(nodeIndex);
        }
        
        C bestCost = null;
        int bestSubmask = -1;
        
        // перебор всех подмасок (http://e-maxx.ru/algo/all_submasks) суммарно за O(3^n)
        for (int submask = mask; submask != 0; submask = (submask - 1) & mask) {
            if (submask != mask) {
                int submask2 = mask ^ submask;
                if (submask < submask2) { // для исключения одинаковых разбиений (предполагаем, что функция оценки коммутативна)
                    Node<V, C> firstNode = DP(submask, memo, functor);
                    Node<V, C> secondNode = DP(submask2, memo, functor);
                    EdgeLinkedList<Edge<V>> edgeList = new EdgeLinkedList<>();
                    for (int firstIndex = 0; firstIndex < nodes.size(); ++firstIndex) {
                        if ((submask & (1<<firstIndex)) != 0) {
                            for (Edge<V> edge : adjList.get(firstIndex)) {
                                int secondIndex;
                                if (vertexIndex.get(edge.getTo()) == firstIndex) {
                                    secondIndex = vertexIndex.get(edge.getFrom());
                                } else {
                                    secondIndex = vertexIndex.get(edge.getTo());
                                }
                                if ((submask2 & (1<<secondIndex)) != 0) {
                                    edgeList.addEdge(edge);
                                }
                            }
                        }
                    }
                    C curCost = functor.calculate(firstNode, secondNode, edgeList);
                    if (bestCost == null || bestCost.compareTo(curCost) > 0) {
                        bestCost = curCost;
                        bestSubmask = submask;
                    }
                }
            }
        }
        
        Node<V, C> resNode = new Node<>(null, bestCost, memo.get(bestSubmask), memo.get(mask ^ bestSubmask));
        memo.set(mask, resNode);
        return resNode;
    }
    
    public Node<V, C> computeDP(CalculateCost<V, C> functor) {
        initNodes();
        assert nodes.size() <= 12;
        int subsetsCount = (1 << nodes.size());
        return DP(subsetsCount - 1, new ArrayList<>(Collections.<Node<V, C>>nCopies(subsetsCount, null)), functor);
    }
    
    public static void drawTree(Node node) {
        System.out.print("(");
        if (node.left == null) {
            System.out.print(node.getVertex().toString());
        } else {
            drawTree(node.left);
            drawTree(node.right);    
        }
        System.out.print(")");
    }
    
    private static class WeightedEdge<T> extends SimpleEdge<T> {
        int w;
        
        public WeightedEdge(T from, T to, int weight) {
            super(from, to);
            w = weight;
        }
    }
    
    public static void test() {
        GreedyTreeBuilding<Integer, Integer> algo = new GreedyTreeBuilding<>();
        int cnt = 5;
        Integer[] num = {0, 1, 2, 3, 4}; 
        
        for (int i = 0; i < cnt; ++i) {
            algo.addVertex(num[i], 0);
        }
        
        algo.addEdge(new WeightedEdge<>(num[0], num[1], 5));
        algo.addEdge(new WeightedEdge<>(num[0], num[2], 10));
        algo.addEdge(new WeightedEdge<>(num[1], num[2], 6));
        algo.addEdge(new WeightedEdge<>(num[1], num[3], 4));
        algo.addEdge(new WeightedEdge<>(num[2], num[3], 2));
        algo.addEdge(new WeightedEdge<>(num[2], num[4], 5));
        algo.addEdge(new WeightedEdge<>(num[3], num[4], 4));
        
        Node<Integer, Integer> res = algo.compute(new CalculateCost<Integer, Integer>() {
            @Override
            public Integer calculate(Node<Integer, Integer> a, Node<Integer, Integer> b, Iterable<Edge<Integer>> edges) {
                if (!edges.iterator().hasNext()) return 100;
                int minw = 100;
                for (Edge<Integer> e : edges) minw = a.getCost() + b.getCost() + Math.min(minw, ((WeightedEdge<Integer>)e).w);
                return minw;  
            }
        });

        Node<Integer, Integer> res2 = algo.computeDP(new CalculateCost<Integer, Integer>() {
            @Override
            public Integer calculate(Node<Integer, Integer> a, Node<Integer, Integer> b, Iterable<Edge<Integer>> edges) {
                assert a != null && b != null;
                if (!edges.iterator().hasNext()) return 100;
                int minw = 100;
                for (Edge<Integer> e : edges) minw = a.getCost() + b.getCost() + Math.min(minw, ((WeightedEdge<Integer>)e).w);
                return minw;
            }
        });
        
        System.out.println("!!!!!!! " + res.getCost());
        drawTree(res);
        System.out.println();
        System.out.println("!!!!!!! " + res2.getCost());
        drawTree(res2);
        System.out.println(); 
    }
}

%nodes is a matrix of n*3, 
%the first column is the nodeId
%the second column is the x coordinates
%the third column is the y coordinates
function group1 = spectralPartition(nodes, adj)
    gplot(adj, [nodes(:, 2), 500-nodes(:, 3)]);
    hold on;
    numNodes = size(adj, 1);
    deg = zeros(numNodes, numNodes);
    for i=1:numNodes
        deg(i, i) = sum(adj(i, :));
    end
    laplacian = deg - adj;
    [V, D] = eig(laplacian);
    eigValues = zeros(1, numNodes);
    for i=1:numNodes
        eigValues(i) = D(i, i);
    end
    [eigValues, ids] = sort(eigValues);
    eigVector = V(:, ids(2));
    [tmp, ids] = sort(eigVector);
    group1 = ids(1:floor(length(ids)/2));
    for i = 1:length(group1)
        co = nodes(group1(i), 2:3);
        plot(co(1), 500-co(2), 'rx');
    end
    group1 = nodes(group1, 1);
end
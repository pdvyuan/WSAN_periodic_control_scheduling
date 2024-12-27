function d = computeSumDist(sensors, gws, clusters)
    d = 0;
    numGWs = size(gws, 1);
    for i=1:numGWs
        gwPos = gws(i, :);
        cluster = clusters{i};
        for j=1:length(cluster)
            nodeId = cluster(j);
            nodePos = sensors(nodeId, 1:2);
            period = sensors(nodeId, 3);
            d = d + eucliddist(nodePos, gwPos)./period;
        end
    end
end
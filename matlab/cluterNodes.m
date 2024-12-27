function clusters = cluterNodes(gateways, sensors)
% clusterNode, cluster a number of nodes to their nearest GWs.
% gateways, the gateway positions a matrix of n*2.
% the sensor node coordinates.
% clusters is an array of vectors, each item contains the indices of all
% clustered nodes.
numGWs = size(gateways, 1);
clusters = cell(numGWs, 1);
numSensors = size(sensors, 1);
for i = 1:numSensors
    nodePos = sensors(i, :);
    gw = 0;
    mindist = bitmax;
    for j = 1:numGWs
        gwPos = gateways(j, :);
        dist = eucliddist(nodePos, gwPos);
        if (dist < mindist)
            mindist = dist;
            gw = j;
        end
    end
    clusters{gw} = [clusters{gw}, i];
end

%sort clusters
for i=1:numGWs
    clusters{i} = sort(clusters{i});
end
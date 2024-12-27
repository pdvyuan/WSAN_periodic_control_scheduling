function gwsPos = computeGWsMinSumDist(sensors, num, maxx, maxy)
%computeGWsMinSumDist place num of GWs that minize the sum distance between
% all source and destination sensor nodes to the nearest GWs.
%   sensors is a n*3 matrix. 
%   row i represents the sensor i, the values correspond to the
%   x-coordinate, y coordinate and the period of a sensor node.
%   maxx, the width of the space. maxy, the height of the space.
    try_times = 10;
    mindist = bitmax;
    for i=1:try_times
        gws = [rand(num, 1)*maxx, rand(num, 1)*maxy];
        clusters = cell(num, 1);
        while (1)
            c = cluterNodes(gws, sensors);
            if (sameCluster(c, clusters))
                break;
            else
                clusters = c;
            end
            for i=1:num
                if (~isempty(clusters{i}))
                    gw = replaceGateway(sensors(clusters{i}, :), gws(i, :));
                    gws(i, :) = gw;
                end
            end
        end
        d = computeSumDist(sensors, gws, clusters);
        if (d < mindist)
            %fprintf('%f, %f\n', d, mindist);
            mindist = d;
            gwsPos = gws;
            clustersResults = clusters;
        end
    end
end
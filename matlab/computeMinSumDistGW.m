function gw = computeMinSumDistGW(sensors, maxx, maxy)
%computeGWsMinSumDist place num of GWs that minize the sum distance between
% all source and destination sensor nodes to the nearest GWs.
%   sensors is a n*3 matrix. 
%   row i represents the sensor i, the values correspond to the
%   x-coordinate, y coordinate and the period of a sensor node.
%   maxx, the width of the space. maxy, the height of the space.
    gw = [rand(1)*maxx, rand(1)*maxy];
    gw = replaceGateway(sensors, gw);
end
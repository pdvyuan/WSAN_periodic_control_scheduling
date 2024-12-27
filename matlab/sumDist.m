function [y, g, h] = sumDist(x)
    global nodes_of_interest;
    numNodes = size(nodes_of_interest, 1);
    y = 0;
    weights = 0;
    g = zeros(1, 2);
    h = zeros(2, 2);
    for i=1:numNodes
        pos = nodes_of_interest(i, 1:2);
        period = nodes_of_interest(i, 3);
        d = eucliddist(x, pos);
        weights = weights + 1/period;
        y = y + d/period;
        g = g + (x - pos) / (period*d);
        h(1, 1) = h(1, 1) + (1/d - (x(1) - pos(1))^2/d^3) / period;
        h(1, 2) = h(1, 2) - ((x(1) - pos(1)) * (x(2) - pos(2))/d^3) / period;
        h(2, 1) = h(1, 2);
        h(2, 2) = h(2, 2) + (1/d - (x(2) - pos(2))^2/d^3) / period;
    end
    y = y/weights;
    g = g/weights;
    h = h/weights;
end
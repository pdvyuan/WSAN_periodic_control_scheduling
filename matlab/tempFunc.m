x = importdata('D:\pdv\workspace\realtime-tdma\log\redo\routing\effectOfSortingFlows\convert.txt');
data = x.data;
for routing = [6, 9]
    routables = zeros(3, 2);
    feasibles = zeros(3, 2);
    reliabilities = zeros(3, 2);
    costs = zeros(3, 2);
    rdata = data(data(:, 4) == routing, :);
    fprintf('routing method %d\n', routing);
    for i=1:size(rdata, 1)
        gw = rdata(i, 2)/2;
        %routable 
        if (rdata(i, 6) == rdata(i, 5))
            routeSeq = rdata(i, 10)+1;
            routables(routeSeq, gw) = routables(routeSeq, gw) + 1;
            costs(routeSeq, gw) = costs(routeSeq, gw)+ rdata(i, 8);
            reliabilities(routeSeq, gw) = reliabilities(routeSeq, gw) + rdata(i, 7);
            if (sum(rdata(i, 11:26)) > 0)
                feasibles(routeSeq, gw) = feasibles(routeSeq, gw)+1;
            end
        end
    end
    routables
    feasibles
    costs./routables
    reliabilities./routables
    for i=1:3:size(rdata, 1)
        if (sum(rdata(i, 11:26)) == 0) && (sum(rdata(i+1, 11:26)) > 0)
            fprintf('low-period-1st < high-period-1st, %d vs %d, r: %.3f vs %.3f, c: %.3f vs %.3f\n', ...
            rdata(i, 6), rdata(i+1, 6), rdata(i, 7), rdata(i+1, 7), rdata(i, 8), rdata(i+1, 8));
        elseif (sum(rdata(i, 11:26)) > 0) && (sum(rdata(i+1, 11:26)) == 0)
            fprintf('low-period-1st > high-period-1st, %d vs %d, r: %.3f vs %.3f, c: %.3f vs %.3f\n', ...
            rdata(i, 6), rdata(i+1, 6), rdata(i, 7), rdata(i+1, 7), rdata(i, 8), rdata(i+1, 8));
        end
    end
end
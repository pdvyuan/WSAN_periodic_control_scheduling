function [routables, feasibles, reliabilities, costs, times] = compareRouting(realistic)
    if (realistic)
        file = 'D:\pdv\workspace\realtime-tdma\log\redo\routing\real\convert.txt';
    else
        file = 'D:\pdv\workspace\realtime-tdma\log\redo\routing\disk\convert.txt';
    end
    data = extract_data(file);
    descs = {'most-reliable', 'shortest-path', 'shortest-path-dinic',...
        'min-sum-loss-rate', 'max-prod-rec-rate', 'min-sum-etx',...
         'min-sum-hops', 'most-reliable-lc', 'most-reliable-bf'};
    totalRoutings = 9;
    routables = zeros(2, totalRoutings);
    feasibles = zeros(2, totalRoutings);
    for routing=1:totalRoutings
        for gwId = 1:2
            gw = gwId * 2;
            pdata = data(data(:, 4) == routing & data(:, 2) == gw, :);
            f = sum(sum(pdata(:, 10:25), 2) ~= 0) / size(pdata, 1);
            feasibles(gwId, routing) = f;
            r = sum(pdata(:, 5) == pdata(:, 6)) / size(pdata, 1);
            routables(gwId, routing) = r;
        end
    end
    figure;
    subplot(5, 1, 1);
    bar(routables * 100);
    set(gca,'XTickLabel', {'2 GWs', '4 GWs'});
    legend(descs);
    ylabel('routable rate');
    
    subplot(5, 1, 2);
    bar(feasibles * 100);
    set(gca,'XTickLabel', {'2 GWs', '4 GWs'});
    ylabel('feasible rate');
    
    reliabilities = zeros(2, totalRoutings);
    costs = zeros(2, totalRoutings);
    times = zeros(2, totalRoutings);
    rData = data(sum(data(:, 10:25), 2) ~= 0, :);
    for routing=1:totalRoutings
        for gwId = 1:2
            gw = gwId * 2;
            r = mean(rData(rData(:, 4) == routing & rData(:, 2) == gw, 7));
            reliabilities(gwId, routing) = r;
            c = mean(rData(rData(:, 4) == routing & rData(:, 2) == gw, 8));
            costs(gwId, routing) = c;
            t = mean(rData(rData(:, 4) == routing & rData(:, 2) == gw, 9));
            times(gwId, routing) = t;
        end
    end
    
    subplot(5, 1, 3);
    bar(reliabilities);
    set(gca,'XTickLabel', {'2 GWs', '4 GWs'});
    ylabel('reliability');
    
    subplot(5, 1, 4);
    bar(costs);
    set(gca,'XTickLabel', {'2 GWs', '4 GWs'});
    ylabel('cost');
    
    subplot(5, 1, 5);
    bar(times./10^6);
    set(gca,'XTickLabel', {'2 GWs', '4 GWs'});
    ylabel('time (ms)');
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end
function [utilmat, feasibles, times, timesStd, maxBuffers, prods] = plotSchedulability(implicit, physical, toPlot)
    if (nargin == 2)
        toPlot = 1;
    end
    descs = {'RM','DM','PDM','CLLF','EDF','LLF-RC','EPD', 'EDZL', 'ALICE', 'TASA', 'RANDOM'};
    if (implicit)
        str = 'implicit deadline';
    else
        str = 'restricted deadline';
    end
    
    if (physical)
        str = [str];
    else
        str = [str ', disk model'];
    end
    
    file = '../log/revision/algorithms/';
    if (implicit)
        file = [file, 'implicit/'];
    else
        file = [file, 'restricted/'];
    end
    if (physical)
        file = [file, 'physical/'];
    else
        file = [file, 'random/'];
    end
    file = [file, 'convert.txt'];
    
    feasibles = [];
    for i=0:4
        ch = 2^i;
        rowv = loadFeasibilityData(extract_data(file), ch);
        feasibles(i+1, :) = rowv;
    end
    feasibles = feasibles * 100;
    % cumulative schedulable rate plots
    if (toPlot)
        f = figure('Position', [100, 100, 800, 504]);
        bar(feasibles');
        grid on;
        %title(str);
        ylabel('schedulability ratio (%)');
        legend('1 ch.', '2 ch.', '4 ch.', '8 ch.', '16 ch.');
        set(gca,'XTickLabel', descs, 'FontSize', 15);
    end
    
    data = extract_data(file);
    %actual utilization, channel.
    data = data(:, [5, 6, 47]);
    data(data == -1) = 0;
    
    stepLen = 0.5;
    channels = data(:, 2);
    utilmat = [];
    for i=0:4
        ch = 2^i;
        %pdata contains floored utils and feasible.
        pdata = data(channels == ch, [1, 3]);
        utilbar = [];
        minV = 0;
        while (minV + stepLen <= 16)
            x = pdata((pdata(:, 1) > minV & pdata(:, 1) <= (minV+stepLen)), 2);
            if (length(x) > 0) 
                utilbar = [utilbar, sum(x) / length(x)];
            else
                utilbar = [utilbar, 0];
            end
            
            minV = minV + stepLen;
        end
        utilmat = [utilmat; utilbar];
    end
    utilmat = utilmat * 100;
    
    if (toPlot)
        f = figure('position',[200 500 900 450]);
        set(gca, 'FontSize', 15);
        hold on;
        xs = stepLen/2:stepLen:16;
        for i=1:size(utilmat, 1)
            ys = utilmat(i, :);
            if (i == 1)
                plot(xs, ys, '+-');
            elseif (i == 2)
                plot(xs, ys, 'o-');
            elseif (i == 3)
                plot(xs, ys, 'x-');
            elseif (i == 4)
                plot(xs, ys, 's-');
            else
                plot(xs, ys, '*-');
            end
        end
        xlabel('total utilization');
        ylabel('schedulability ratio w. LLF-RC (%)');
        %title(str);
        legend('1 ch.', '2 ch.', '4 ch.', '8 ch.', '16 ch.');
        grid on;
        xlim([0, 24])
        set(gca,'XTick',0:2:24);
    end
    
    %correlation between flows count with schedulability rate.
%     utilmat = [];
%     for i=0:4
%         ch = 2^i;
%         %pdata contains floored utils and feasible.
%         pdata = data(channels == ch, [4, 3]);
%         utilbar = [];
%         for j=1:50
%             x = pdata(pdata(:, 1) == j, 2);
%             if (length(x) > 0) 
%                 utilbar = [utilbar, sum(x) / length(x)];
%             else
%                 utilbar = [utilbar, 0];
%             end
%         end
%         utilmat = [utilmat; utilbar];
%     end
%     utilmat = utilmat * 100;
%     
%     if (toPlot)
%         figure('position',[200 500 700 350]);
%         hold on;
%         xs = 1:50;
%         for i=1:size(utilmat, 1)
%             ys = utilmat(i, :);
%             if (i == 1)
%                 plot(xs, ys, '+-');
%             elseif (i == 2)
%                 plot(xs, ys, 'o-');
%             elseif (i == 3)
%                 plot(xs, ys, 'x-');
%             elseif (i == 4)
%                 plot(xs, ys, 's-');
%             else
%                 plot(xs, ys, '.-');
%             end
%         end
%         xlabel('channels');
%         ylabel('schedulable rate w. LLF (%)');
%         title(str);
%         legend('1 ch.', '2 ch.', '4 ch.', '8 ch.', '16 ch.');
%         grid on;
%     end
    
    numSchedulers = size(descs, 2);
    times = zeros(1, numSchedulers);
    timesStd = zeros(1, numSchedulers);
    maxBuffers = zeros(2, numSchedulers);
    prods = zeros(2, numSchedulers);
    data = extract_data(file);
    data = [data(:, 1:31), data(:, 47:51), data(:, 37:46), data(:, 52:66)];
    
    for i=1:numSchedulers
        %find the cases that problems are schedulable by all.
        ids = find(sum(data(:, 7:5:size(data, 2)), 2) == numSchedulers);
        times(i) = mean(data(ids, 3+5*i));
        timesStd(i) = std(data(ids, 3+5*i));
        maxBuffers(1, i) = mean(data(ids, 4+5*i));
        maxBuffers(2, i) = std(data(ids, 4+5*i));
        prods(1, i) = mean(data(ids, 5+5*i));
        prods(2, i) = std(data(ids, 5+5*i));
    end
end

function data = extract_data(file)
    x = importdata(file);
    data = x.data;
end

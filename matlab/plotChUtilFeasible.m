function plotChUtilFeasible()
    figure;
    hold on;
    channels = [1 2 4 8 12 16];
    for ch = channels
        file = ['D:\pdv\workspace\realtime-tdma\log\implicit\grid', num2str(ch), 'ch2gw\convert.txt'];
        ch
        plotFeasibleLine(file, 'b', ch);
    end
    xlabel('channels');
    ylabel('total utilization');
    zlabel('feasibility rate');
    grid on;
end

function plotFeasibleLine(file, color, channels)
    x = importdata(file);
    data = x.data;
    data(data == -1) = 0;
    minu = floor(min(data(:, 5)));
    maxu = floor(max(data(:, 5)));
    
    aus = data(:, 5);
	scheduler = 5;
    res = data(:, 5+scheduler);
    feasi = zeros(1, maxu-minu+1);
    totals = zeros(1, maxu-minu+1);
    for i=1:size(data, 1)
        id = floor(aus(i))-minu+1;
        if (res(i) ~= 0)
            feasi(id) = feasi(id)+1;
        end
        totals(id) = totals(id)+1;
    end
    rates = feasi ./ totals;
    xs = minu+0.5:maxu+0.5;
    ids = ~isnan(rates);
    rates = rates(ids);
    xs = xs(ids);
    chs = ones(size(xs)).*channels;
    h = plot3 (chs, xs, rates);
    set(h, 'Color', color);
end